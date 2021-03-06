package nitrous.mbc;

import nitrous.cpu.Emulator;
import nitrous.cpu.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static nitrous.cpu.R.*;

/**
 * Represents the memory of a Gameboy.
 * <p>
 * The Gameboy has a 16-bit address bus, allowing it to use $0000-FFFF. Some parts of this memory range
 * are switchable, and may either be handled by the Gameboy (e.g., work ram on the Gameboy Color), or the
 * game cartridge (e.g., ROM banking).
 * </p>
 * General Memory Map
 * 0000-3FFF   16KB ROM Bank 00     (in cartridge, fixed at bank 00)
 * 4000-7FFF   16KB ROM Bank 01..NN (in cartridge, switchable bank number)
 * 8000-9FFF   8KB Video RAM (VRAM) (switchable bank 0-1 in CGB Mode)
 * A000-BFFF   8KB External RAM     (in cartridge, switchable bank, if any)
 * C000-CFFF   4KB Work RAM Bank 0 (WRAM)
 * D000-DFFF   4KB Work RAM Bank 1 (WRAM)  (switchable bank 1-7 in CGB Mode)
 * E000-FDFF   Same as C000-DDFF (ECHO)    (typically not used)
 * FE00-FE9F   Sprite Attribute Table (OAM)
 * FEA0-FEFF   Not Usable
 * FF00-FF7F   I/O Ports
 * FF80-FFFE   High RAM (HRAM)
 * FFFF        Interrupt Enable Register
 *
 * @author Tudor
 * @author Quantum
 */
public class Memory
{
    /**
     * Size of a page of Video RAM, in bytes. 8kb.
     */
    public static final int VRAM_PAGESIZE = 0x2000;

    /**
     * Size of a page of Work RAM, in bytes. 4kb.
     */
    public static final int WRAM_PAGESIZE = 0x1000;

    /**
     * Size of a page of ROM, in bytes. 16kb.
     */
    public static final int ROM_PAGESIZE = 0x4000;

    /**
     * Register values, mapped from $FF00-$FF7F + HRAM ($FF80-$FFFE) + Interrupt Enable Register ($FFFF)
     */
    public final byte[] registers = new byte[0x100];

    /**
     * Sprite Attribute Table, mapped from $FE00-$FE9F.
     */
    public final byte[] oam = new byte[0xA0];

    /**
     * Video RAM, mapped from $8000-$9FFF.
     * <p/>
     * On the GBC, this bank is switchable 0-1 by writing to $FF4F.
     */
    public final byte[] vram;

    /**
     * Work RAM, mapped from $C000-$CFFF and $D000-$DFFF.
     * <p/>
     * On the GBC, this bank is switchable 1-7 by writing to $FF07.
     */
    public final byte[] wram;

    /**
     * The current page of Video RAM, always multiples of Memory.VRAM_PAGESIZE.
     * <p/>
     * On non-GBC, this is always 0.
     */
    public int vramPageStart = 0;

    /**
     * The current page of Work RAM, always multiples of Memory.WRAM_PAGESIZE.
     * <p/>
     * On non-GBC, this is always Memory.VRAM_PAGESIZE.
     */
    public int wramPageStart = WRAM_PAGESIZE;

    /**
     * The current page of ROM, always multiples of Memory.ROM_PAGESIZE.
     */
    public int romPageStart = ROM_PAGESIZE;

    /**
     * Reference to the main Emulator instance.
     */
    protected final Emulator core;

    /**
     * Current HDMA session (always null on non-CGB).
     */
    public HDMA hdma;

    /**
     * Instantiate a Memory instance.
     *
     * @param core the Emulator to bind to.
     */
    public Memory(Emulator core)
    {
        this.core = core;

        // The CGB has 16k of wram
        wram = new byte[WRAM_PAGESIZE * (core.cartridge.isColorGB ? 8 : 2)];

        // and 8k of vram
        vram = new byte[VRAM_PAGESIZE * (core.cartridge.isColorGB ? 2 : 1)];
    }

    /**
     * Convenience method for determining whether the current cartridge supports saving (i.e., whether or not
     * it has a battery).
     *
     * @return true if it does, false otherwise.
     */
    public boolean hasBattery()
    {
        return core.cartridge.hasBattery();
    }

    /**
     * Writes cart ram to the given OutputStream.
     *
     * @param out where to write raw cart ram.
     * @throws IOException                   if something goes wrong.
     * @throws UnsupportedOperationException if this cart has no battery.
     */
    public void save(OutputStream out) throws IOException
    {
        throw new UnsupportedOperationException("no battery");
    }

    /**
     * Represents a H-Blank DMA transfer session.
     * <p/>
     * HDMA transfers 16 bytes from source to dest every H-Blank interval,
     * and can be used for a lot of video effects.
     */
    public final class HDMA
    {
        /**
         * The source address.
         */
        private final int source;

        /**
         * The destination address.
         */
        private final int dest;

        /**
         * The length of the transfer.
         */
        private int length;

        /**
         * The current offset into the source/dest buffers.
         */
        private int ptr;

        /**
         * Creates a new HDMA instance.
         *
         * @param source The source address to copy from.
         * @param dest   The destination address to copy to.
         * @param length How many bytes to copy.
         */
        public HDMA(int source, int dest, int length)
        {
            this.source = source;
            this.dest = dest;
            this.length = length;
        }

        /**
         * Ticks DMA.
         */
        public void tick()
        {
            /**
             * The H-Blank DMA transfers 10h bytes of data during each H-Blank, ie. at LY=0-143, no data is transferred
             * during V-Blank (LY=144-153), but the transfer will then continue at LY=00. The execution of the program
             * is halted during the separate transfers, but the program execution continues during the 'spaces'
             * between each data block.
             *
             * Note that the program may not change the Destination VRAM bank (FF4F), or the Source ROM/RAM bank
             * (in case data is transferred from bankable memory) until the transfer has completed!
             * Reading from Register FF55 returns the remaining length (divided by 10h, minus 1), a value of 0FFh
             * indicates that the transfer has completed. It is also possible to terminate an active H-Blank transfer
             * by writing zero to Bit 7 of FF55. In that case reading from FF55 may return any value for the
             * lower 7 bits, but Bit 7 will be read as "1".
             */
            for (int i = ptr; i < ptr + 0x10; i++)
            {
                vram[vramPageStart + dest + i] = (byte) (getAddress(source + i) & 0xff);
            }

            ptr += 0x10;
            length -= 0x10;
            System.err.printf("Ticked HDMA from %04X-%04X, %02X remaining\n", source, dest, length);
            if (length == 0)
            {
                Memory.this.hdma = null;
                registers[0x55] = (byte) 0xff;
                System.err.printf("Finished HDMA from %04X-%04X\n", source, dest);
            } else
            {
                registers[0x55] = (byte) (length / 0x10 - 1);
            }
        }
    }

    /**
     * Loads cart ram from the given InputStream.
     *
     * @param in where to read raw cart ram from.
     * @throws IOException                   if something goes wrong.
     * @throws UnsupportedOperationException if this cart has no battery, and hence no cart ram.
     */
    public void load(InputStream in) throws IOException
    {
        throw new UnsupportedOperationException("no battery");
    }

    /**
     * Sets a byte of data.
     *
     * #method
     *
     * @param addr  The address to which to write to.
     * @param _data The data.
     */
    public void setAddress(int addr, int _data)
    {
        byte data = (byte) (_data & 0xff);
        addr &= 0xFFFF;
        int block = addr & 0xF000;
        switch (block)
        {
            case 0x0000:
            case 0x1000:
            case 0x2000:
            case 0x3000:
            case 0x4000:
            case 0x5000:
            case 0x6000:
            case 0x7000:
                // handled by external hardware, if any
                break;
            case 0x8000:
            case 0x9000:
                vram[vramPageStart + addr - 0x8000] = data;
                break;
            case 0xA000:
            case 0xB000:
                // handled by external hardware, if any
                break;
            case 0xC000:
                wram[addr - 0xC000] = data;
                break;
            case 0xD000:
                wram[wramPageStart + addr - 0xD000] = data;
                break;
            case 0xE000:
            case 0xF000:
                // FEA0-FEFF is not usable
                if (0xFEA0 <= addr && addr <= 0xFEFF) break;
                if (addr < 0xFE00)
                {
                    // 7.5kb echo
                    setAddress(addr - 0xE000, data);
                } else if (addr < 0xFF00)
                {
                    oam[addr - 0xFE00] = data;
                } else
                {
                    setIO(addr - 0xFF00, data);
                }
                break;
        }
    }

    /**
     * Sets a register.
     *
     * @param addr The address of the register (00h+).
     * @param data The data to set.
     */
    public void setIO(int addr, int data)
    {
        switch (addr)
        {
            case 0x4d:
                core.setDoubleSpeed((data & 0x01) != 0);
                break;
            case 0x69:
            {
                if (!core.cartridge.isColorGB) break;
                int ff68 = registers[0x68];
                int currentRegister = ff68 & 0x3f;
                core.lcd.setBackgroundPalette(currentRegister, data);
                if ((ff68 & 0x80) != 0)
                {
                    currentRegister++;
                    currentRegister %= 0x40;
                    registers[0x68] = (byte) (0x80 | currentRegister);
                }
                break;
            }
            case 0x6b:
            {
                if (!core.cartridge.isColorGB) break;
                int ff6a = registers[0x6a];
                int currentRegister = ff6a & 0x3f;
                core.lcd.setSpritePalette(currentRegister, data);
                if ((ff6a & 0x80) != 0)
                {
                    currentRegister++;
                    currentRegister %= 0x40;
                    registers[0x6a] = (byte) (0x80 | currentRegister);
                }
                break;
            }
            case 0x55: // HDMA start
            {
                if (!core.cartridge.isColorGB) break;
                int length = ((data & 0x7f) + 1) * 0x10;
                int source = ((registers[0x51] & 0xff) << 8) | (registers[0x52] & 0xF0);
                int dest = ((registers[0x53] & 0x1f) << 8) | (registers[0x54] & 0xF0);
                if ((data & 0x80) != 0)
                {
                    // H-Blank DMA
                    hdma = new HDMA(source, dest, length);
                    registers[0x55] = (byte) (length / 0x10 - 1);
                    break;
                } else
                {
                    if (hdma != null)
                    {
                        System.err.printf("!!! Terminated HDMA from %04X-%04X, %02X remaining\n", source, dest, length);
                    }

                    // General DMA
                    for (int i = 0; i < length; i++)
                    {
                        vram[vramPageStart + dest + i] = (byte) (getAddress(source + i) & 0xff);
                    }
                    registers[0x55] = (byte) 0xFF;
                }
                break;
            }
            case R_VRAM_BANK:
            {
                if (core.cartridge.isColorGB)
                {
                    vramPageStart = VRAM_PAGESIZE * (data & 0x3);
                }
                break;
            }
            case R_WRAM_BANK:
            {
                if (core.cartridge.isColorGB)
                {
                    wramPageStart = WRAM_PAGESIZE * Math.max(1, data & 0x7);
                }
                break;
            }
            case R_NR14:
                if ((registers[R_NR14] & 0x80) != 0)
                {
                    data &= 0x7f;
                }
            case R_NR10:
            case R_NR11:
            case R_NR12:
            case R_NR13:
                registers[addr] = (byte) data;
                break;
            case R_NR24:
                if ((data & 0x80) != 0)
                {
                    data &= 0x7F;
                }
            case R_NR21:
            case R_NR22:
            case R_NR23:
                registers[addr] = (byte) data;
                break;
            case R_NR34:
                if ((data & 0x80) != 0)
                {
                    data &= 0x7F;
                }
            case R_NR30:
            case R_NR31:
            case R_NR32:
            case R_NR33:
                registers[addr] = (byte) data;
                break;
            case R_NR44:
                if ((data & 0x80) != 0)
                {
                    data &= 0x7F;
                }
            case R_NR41:
            case R_NR42:
            case R_NR43:
                registers[addr] = (byte) data;
                break;
            /**
             * Writing to this register launches a DMA transfer from ROM or RAM to OAM memory (sprite attribute table).
             * The written value specifies the transfer source address divided by 100h, ie. source & destination are:
             *
             *   Source:      XX00-XX9F   ;XX in range from 00-F1h
             *   Destination: FE00-FE9F
             *
             * http://hitmen.c02.at/files/releases/gbc/gbc_dma_transfers.txt
             *
             * length:      - always 4*40 (=160 / $a0) bytes
             */
            case R_DMA:
            {
                int addressBase = data * 0x100;

                for (int i = 0; i < 0xA0; i++)
                {
                    setAddress(0xFE00 + i, getAddress(addressBase + i));
                }
                break;
            }

            /**
             * DIV
             * Divider Register (R/W)
             *
             * This register is incremented 16384 times a second.
             * Writing any value sets it to $00
             */
            case R_DIV:
                data = 0;
                break;
            case R_TAC:
                if (((registers[R_TAC] ^ data) & 0x03) != 0)
                {
                    core.timerCycle = 0;
                    registers[R_TIMA] = registers[R_TMA];
                }
                break;
            case R_LCD_STAT:
                break;
        }
        registers[addr] = (byte) data;
    }

    /**
     * Fetches a byte from an address.
     *
     * @param addr The address to fetch from.
     * @return The contained signed value.
     */
    public short getAddress(int addr)
    {
        addr &= 0xFFFF;
        int block = addr & 0xF000;
        switch (block)
        {
            case 0x0000:
            case 0x1000:
            case 0x2000:
            case 0x3000:
                return core.cartridge.rom[addr];
            case 0x4000:
            case 0x5000:
            case 0x6000:
            case 0x7000:
                return core.cartridge.rom[romPageStart + addr - 0x4000];
            case 0x8000:
            case 0x9000:
                return vram[vramPageStart + addr - 0x8000];
            case 0xA000:
            case 0xB000:
                return 0;
            case 0xC000:
                return wram[addr - 0xc000];
            case 0xD000:
                return wram[wramPageStart + addr - 0xd000];
            case 0xE000:
            case 0xF000:
                // FEA0-FEFF is not usable
                if (0xFEA0 <= addr && addr <= 0xFEFF) return 0xFF;
                if (addr < 0xFE00)
                {
                    // E000-FE00 echoes the main ram
                    // But wait, E000-FE00 contains just 7.5kb and hence
                    // does not echo the entire 8kb internal ram
                    return getAddress(addr - 0xE000);
                } else if (addr < 0xFF00)
                {
                    return oam[addr - 0xFE00];
                } else
                {
                    return getIO(addr - 0xFF00);
                }
        }
        return 0xFF;
    }

    /**
     * Reads a register.
     *
     * @param addr The address of the register.
     * @return The signed value contained in the register.
     */
    public short getIO(int addr)
    {
        addr &= 0xFFFF;
        switch (addr)
        {
            case 0x4d:
                if (core.isDoubleSpeed()) return 0x80;
                return 0;
            case 0x00: // JOYPAD (FIXME not done)
            {
                byte reg = registers[0x00];
                short output = 0x0F;
                switch ((reg & 0b110000) >> 4)
                {
                    case 1:
                        if (core.buttonA) output &= ~0x01;
                        if (core.buttonB) output &= ~0x02;
                        if (core.buttonSelect) output &= ~0x04;
                        if (core.buttonStart) output &= ~0x08;
                        break;
                    case 2:
                    case 3:
                        if (core.buttonRight) output &= ~0x1;
                        if (core.buttonLeft) output &= ~0x2;
                        if (core.buttonUp) output &= ~0x4;
                        if (core.buttonDown) output &= ~0x8;
                        break;
                }

                // keep the last 2 bits as-is, in case someone wrote to them
                // I'm not sure if this is correct, but if its not it probably doesn't matter
                return (short) ((0x30 | output | (reg & 0b1100000)) & 0xff);
            }
            case R_NR52:
            {
                short reg = (short) (registers[R_NR52] & 0x80);
                return reg;
            }
        }
        return registers[addr];
    }
}
