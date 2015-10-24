package nes;

import debug.Debug;
import debug.MemoryViewer;


public class Memory {
	NESContext context;
	byte[] data;
	
	public Memory(NESContext context){
		this.context = context;
		data = new byte[0x10000];
	}
	
	private interface MemRegion{
		int read(int addr);
		void write(int addr, byte data);
	}
	
	//0x0000 - 0x1fff
	MemRegion RAM = new MemRegion(){
		public int read(int addr){
			return data[addr%0x800];
		}
		public void write(int addr, byte data){
			Memory.this.data[addr%0x800] = data;
		}
	};
	
	//0x2000 - 0x3fff
	MemRegion Registers = new MemRegion(){
		public int read(int addr) {
			int a = addr%0x8;
			switch(a){
			case 0x0:case 0x1:
			case 0x3:
			case 0x4:
				return context.ppu.getSPRRAM().read();
			case 0x5:case 0x6:
				Debug.println("Unexpected read from " + Integer.toHexString(addr));
				break;
			case 0x2:
				Debug.println("context.ppu.readStatusRegister : ");
				return context.ppu.readStatusRegister()&0xff;
			case 0x7:
				Debug.println("context.ppu.readVram : ");
				return context.ppu.readVram();
			}
			return 0;
		}
		public void write(int addr, byte data) {
			int a = addr&0x7;
			switch(a){
			case 0x0:
				Debug.println("context.ppu.writeControlRegister1 : "+Debug.hex2(data));
				context.ppu.writeControlRegister1(data);
				break;
			case 0x1:
				Debug.println("context.ppu.writeControlRegister2 : "+Debug.hex2(data));
				context.ppu.writeControlRegister2(data);
				break;
			case 0x2:
				Debug.println("Unexpected write to " + Integer.toHexString(addr));
				break;
			case 0x3:
				Debug.println("context.ppu.getSPRRAM().setAddress : "+Debug.hex2(data));
				context.ppu.getSPRRAM().setAddress(data);
				break;
			case 0x4:
				Debug.println("context.ppu.getSPRRAM().write : "+Debug.hex2(data));
				context.ppu.getSPRRAM().write(data);
				break;
			case 0x5:
				Debug.println("context.ppu.setScroll : "+Debug.hex2(data));
				context.ppu.setScroll(data);
				break;
			case 0x6:
				Debug.println("context.ppu.setVramAddr : "+Debug.hex2(data));
				context.ppu.setVramAddr(data);
				break;
			case 0x7:
				Debug.println("context.ppu.writeVram : "+Debug.hex2(data));
				context.ppu.writeVram(data);
				break;
			}
		}
	};
	
	//0x4000 - 0x401f
	MemRegion Registers2 = new MemRegion(){
		public int read(int addr) {
			if(addr == 0x4016){
				//TODO Zapper not implemented
				return context.joypad.read();
			}else if(addr == 0x4017){
				//TODO JoyPad2/Zapper, SOFTCLC not implemented
				return 0;//context.joypad.read();
			}else{
				Debug.println("Not Implemented : read " + Integer.toHexString(addr));
				return 0;
			}
		};
		public void write(int addr, byte data) {
			if(addr == 0x4014){//sprite DMA
				context.ppu.getSPRRAM().dma(Memory.this.data, (0xff&data)<<8);
			}else if(addr == 0x4016){
//				if((data&1) == 0)
					context.joypad.readReset();
			}else if(addr == 0x4017){
				//TODO JOYPAD2/SOFTCLC not implemented
			}else{
				Debug.println("Not Implemented : write " + Integer.toHexString(addr));
			}
		};
	};
	
	//0x4020 - 0x5fff
	MemRegion ExpansionROM = new MemRegion(){
		public int read(int addr) {
			Debug.println("Not Implemented : read " + Integer.toHexString(addr));
			return 0;
		};
		public void write(int addr, byte data) {
			Debug.println("Not Implemented : write " + Integer.toHexString(addr));
		};
	};
	
	//0x6000 - 0x7fff
	MemRegion SRAM = new MemRegion(){
		public int read(int addr) {
			Debug.println("Not Implemented : read " + Integer.toHexString(addr));
			return 0;
		};
		public void write(int addr, byte data) {
			Debug.println("Not Implemented : write " + Integer.toHexString(addr));
		};
	};
	
	//0x8000 - 0xbfff
	MemRegion PRGRAM1 = new MemRegion(){
		public int read(int addr) {
			return data[addr]&0xff;
		};
		public void write(int addr, byte data) {
			Debug.println("Unexpected Write to PRGRAM1 : "+Integer.toHexString(addr));
		};
	};
	
	//0xC000 - 0xffff
	MemRegion PRGRAM2 = new MemRegion(){
		public int read(int addr) {
			return data[addr]&0xff;
		};
		public void write(int addr, byte data) {
			Debug.println("Unexpected Write to PRGRAM2 : "+Integer.toHexString(addr));
		};
	};
	
	MemRegion map(int addr){
		if(addr < 0x6000){
			if(addr < 0x2000){
				return RAM;
			}else if(addr < 0x4000){
				return Registers;
			}else if(addr < 0x4020){
				return Registers2;
			}else{
				return ExpansionROM;
			}
		}else{
			if(addr < 0x8000){
				return SRAM;
			}else if(addr < 0xC000){
				return PRGRAM1;
			}else{
				return PRGRAM2;
			}
		}
	}

	//return read value 0-255
	int read(int addr){
//		Debug.print(" MemRead["+Debug.hex4(addr)+"] : ");
		int res = map(addr).read(addr)&0xff;
//		Debug.println(Debug.hex2(res));
		return res;
	}

	void write(int addr, byte data){
//		Debug.println(" MemWrite["+Debug.hex4(addr)+","+Debug.hex2(data)+"] : ");
		map(addr).write(addr, data);
	}
	
	void loadPRGRAM(int i, byte[] data){
		for(int j=0; j<0x4000; j++){
			this.data[0x8000+i*0x4000+j] = data[j];
		}
	}
	MemoryViewer dbgMemView;
	void showDebugWindow(){
		dbgMemView = new MemoryViewer("Memory", data);
	}
}
