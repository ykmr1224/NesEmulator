package nes;
import java.util.HashMap;

import debug.Debug;


public class CPU {
	static final int CLOCK_SPEED = 1790000; //1.79MHz
	
	int a;
	int x, y;
	int pc; // program counter
	int s = 0xff; // stack pointer
	int clock = 0;
	boolean CarryFlag = false;
	boolean ZeroFlag = false;
	boolean InterruptFlag = false;
	boolean DecimalMode = false;
	boolean BreakCommand = false;
	boolean OverFlowFlag = false;
	boolean NegativeFlag = false;
	
	boolean nmiOccuring = false;

	Memory mem;
	
	public CPU(Memory mem){
		this.mem = mem;
	}

	public class Operand{
		int ope;
		String instStr;
		String addrStr;
		Instruction inst;
		Addressing addr;
		int clock;
		public Operand(int ope, String inst, String addr, int clock){
			this.ope = ope;
			this.instStr = inst;
			this.addrStr = addr;
			this.inst = ins.get(inst);
			this.addr = adds.get(addr);
			this.clock = clock;
		}
		@Override
		public String toString() {
			return instStr + " " + addrStr;
		}
	}
	
	// CPU won't count up PC if the jumpFlg is true
	boolean jumpFlg = false;
	
	private void jump(int addr){
		pc = addr;
		jumpFlg = true;
	}
	
	// increments PC
	private void incPC(int i){
		if(jumpFlg){
			jumpFlg = false;
		}else{
			pc += i;
		}
	}
	
	// push i to the stack
	private void push(byte i){
		mem.write(0x0100 + s, i);
		s -= 1;
		s &= 0xff;
	}
	
	// pull byte from the stack
	private int pull(){
		s += 1;
		int res = mem.read(0x0100 + s);
		s &= 0xff;
		return res;
	}

	// push 16bit address to the stack(significant byte first)
	private void pushAddress(int addr){
		push((byte)((addr>>8)&0xff));
		push((byte)(addr&0xff));
	}
	
	// pull 16bit address from the stack
	private int pullAddress(){
		int a = pull()&0xff;
		int b = pull()&0xff;
		return (b<<8)|a;
	}
	
	// get current status byte(NO11DIZC)
	private byte saveStatus(){
		return (byte)(
			((CarryFlag?1:0)) |
			((ZeroFlag?1:0)<<1) |
			((InterruptFlag?1:0)<<2) |
			((DecimalMode?1:0)<<3) |
			(1<<4) |
			(1<<5) |
			((OverFlowFlag?1:0)<<6) |
			((NegativeFlag?1:0)<<7)
			);
	}
	
	// restore status from i(NO11DIZC)
	private void storeStatus(int i){
		CarryFlag = ((i)&1)==1;
		ZeroFlag = ((i>>1)&1)==1;
		InterruptFlag = ((i>>2)&1)==1;
		DecimalMode = ((i>>3)&1)==1;
		OverFlowFlag = ((i>>6)&1) == 1;
		NegativeFlag = ((i>>7)&1) == 1;
	}
	
	// read 16bit data from memory at addr and addr+1(little endian)
	private int read16bit(int addr){
		return mem.read(addr)+(mem.read((addr+1)&0xffff)<<8);
	}

	// read 16bit data from memory at addr and addr+1(wrapped by 256byte memory block)
	private int readWrapped16bit(int addr){
		return mem.read(addr)+(mem.read((addr&0xff00)+((addr+1)&0xff))<<8);
	}
	
	// return bth bit in data (0 or 1)
	private int bit(int data, int b){
		return (data>>b)&1;
	}
	
	public interface Addressing{
		// execute the instruction using this addressing
		void ope(Instruction i);
		// returns the size of instruction bytes
		int getSize();
		// returns the addressing description for debugger
		String toString(byte d0, byte d1);
	}
	
	// addressing modes
	HashMap<String, Addressing> adds = new HashMap<String, Addressing>();

	// build all the addressing modes
	{
		adds.put("Implicit", new Addressing(){
			public void ope(Instruction i){
				i.ope(0);
				incPC(1);
			}
			public int getSize() {
				return 1;
			}
			public String toString(byte d0, byte d1) {
				return "";
			}
		});
		
		adds.put("Accumulator", new Addressing(){
			public void ope(Instruction i){
				i.opeAccumulator();
				incPC(1);
			}
			public int getSize() {
				return 1;
			}
			public String toString(byte d0, byte d1) {
				return "A";
			}
		});

		adds.put("Immediate", new Addressing(){
			public void ope(Instruction i){
				i.ope(pc+1);
				incPC(2);
			}
			public int getSize() {
				return 2;
			}
			public String toString(byte d0, byte d1) {
				return "#$"+Debug.hex2(d0);
			}
		});
		
		adds.put("ZeroPage", new Addressing(){
			public void ope(Instruction i){
				int data = mem.read(pc+1);
				i.ope(data);
				incPC(2);
			}
			public int getSize() {
				return 2;
			}
			public String toString(byte d0, byte d1) {
				return "#00["+Debug.hex2(d0)+"]";
			}
		});

		adds.put("ZeroPageX", new Addressing(){
			public void ope(Instruction i){
				int data = mem.read(pc+1);
				i.ope((data+x)&0xff);
				incPC(2);
			}
			public int getSize() {
				return 2;
			}
			public String toString(byte d0, byte d1) {
				return "#00["+Debug.hex2(d0)+",X]";
			}
		});
		
		adds.put("ZeroPageY", new Addressing(){
			public void ope(Instruction i){
				int data = mem.read(pc+1);
				i.ope((data+y)&0xff);
				incPC(2);
			}
			public int getSize() {
				return 2;
			}
			public String toString(byte d0, byte d1) {
				return "#00["+Debug.hex2(d0)+",Y]";
			}
		});
		
		adds.put("Relative", new Addressing(){
			public void ope(Instruction i){
				int data = (byte)mem.read(pc+1);//-128 .. 127
				i.ope(pc+data);
				incPC(2);
			}
			public int getSize() {
				return 2;
			}
			public String toString(byte d0, byte d1) {
				return "*"+d0+"";
			}
		});
		
		adds.put("Absolute", new Addressing(){
			public void ope(Instruction i){
				int data = read16bit(pc+1);
				i.ope(data);
				incPC(3);
			}
			public int getSize() {
				return 3;
			}
			public String toString(byte d0, byte d1) {
				return "#["+Debug.hex4(((d1&0xff)<<8) | (d0&0xff))+"]";
			}
		});
		
		adds.put("AbsoluteX", new Addressing(){
			public void ope(Instruction i){
				int data = read16bit(pc+1);
				i.ope((data+x)&0xffff);
				incPC(3);
			}
			public int getSize() {
				return 3;
			}
			public String toString(byte d0, byte d1) {
				return "#["+Debug.hex4(((d1&0xff)<<8) | (d0&0xff))+"+X]";
			}
		});
		
		adds.put("AbsoluteY", new Addressing(){
			public void ope(Instruction i){
				int data = read16bit(pc+1);
				i.ope((data+y)&0xffff);
				incPC(3);
			}
			public int getSize() {
				return 3;
			}
			public String toString(byte d0, byte d1) {
				return "#["+Debug.hex4(((d1&0xff)<<8) | (d0&0xff))+"+Y]";
			}
		});
		
		adds.put("Indirect", new Addressing(){
			public void ope(Instruction i){
				int data = read16bit(pc+1);
				// second byte should be wrapped by 256byte memory block(0x33ff + 0x1 -> 0x3300)
				int addr = readWrapped16bit(data);
				i.ope(addr);
				incPC(3);
			}
			public int getSize() {
				return 3;
			}
			public String toString(byte d0, byte d1) {
				return "(#"+Debug.hex4(((d1&0xff)<<8) | (d0&0xff))+")";
			}
		});
		
		adds.put("IndirectX", new Addressing(){
			public void ope(Instruction i){
				int data = mem.read(pc+1);
				int addr = readWrapped16bit((data+x)&0xff);
				i.ope(addr) ;
				incPC(2);
			}
			public int getSize() {
				return 2;
			}
			public String toString(byte d0, byte d1) {
				return "(#00["+Debug.hex2(d0)+"+X])";
			}
		});

		adds.put("IndirectY", new Addressing(){
			public void ope(Instruction i){
				int data = mem.read(pc+1);
				int addr = readWrapped16bit(data&0xff);
				i.ope((addr+y)&0xffff);
				incPC(2);
			}
			public int getSize() {
				return 2;
			}
			public String toString(byte d0, byte d1) {
				return "(#00["+Debug.hex2(d0)+"]+Y)";
			}
		});
	}

	private abstract class Instruction{
		abstract void ope(int i);
		void opeAccumulator(){Debug.println("Not Implemented.");};
	}

	// store all the instructions
	HashMap<String, Instruction> ins = new HashMap<String, Instruction>();
	
	{
		// add
		ins.put("ADC", new Instruction() {
			public void ope(int i) {
				int data = mem.read(i);
				int temp = a + data + (CarryFlag?1:0);
				int temps = (byte)a + (byte)data + (CarryFlag?1:0);
				a = temp & 0xff;
				CarryFlag = bit(temp,8) == 1;
				ZeroFlag = (a==0);
				OverFlowFlag = temps<-128||127<temps;
				NegativeFlag = bit(temp,7) == 1;
			}
		});
		// and
		ins.put("AND", new Instruction() {
			public void ope(int i) {
				a = a&mem.read(i);
				ZeroFlag = (a==0);
				NegativeFlag = bit(a,7) == 1;
			}
		});
		// shift left
		ins.put("ASL", new Instruction() {
			public void ope(int i) {
				int temp = mem.read(i)*2;
				mem.write(i, (byte)temp);
				CarryFlag = bit(temp,8) == 1;
				ZeroFlag = (temp&0xff) == 0;
				NegativeFlag = bit(temp,7) == 1;
			}
			public void opeAccumulator(){
				int temp = a*2;
				a = temp&0xff;
				CarryFlag = bit(temp,8) == 1;
				ZeroFlag = a == 0;
				NegativeFlag = bit(a, 7) == 1;
			}
		});
		// branch if Carry is clear
		ins.put("BCC", new Instruction() {
			public void ope(int i) {
				if(!CarryFlag)
					pc=i;
			}
		});
		// branch if Carry is set
		ins.put("BCS", new Instruction() {
			public void ope(int i) {
				if(CarryFlag)
					pc=i;
			}
		});
		// branch if Zero is set (equal)
		ins.put("BEQ", new Instruction() {
			public void ope(int i) {
				if(ZeroFlag)
					pc=i;
			}
		});
		// bit and
		ins.put("BIT", new Instruction() {
			public void ope(int i) {
				int data = mem.read(i);
				int temp = a & data;
				ZeroFlag = (temp==0);
				OverFlowFlag = bit(data,6) == 1;
				NegativeFlag = bit(data,7) == 1;
			}
		});
		// branch if Negative is clear (minus)
		ins.put("BMI", new Instruction() {
			public void ope(int i) {
				if(NegativeFlag)
					pc=i;
			}
		});
		// branch if Zero is clear (not equal)
		ins.put("BNE", new Instruction() {
			public void ope(int i) {
				if(!ZeroFlag)
					pc=i;
			}
		});
		// branch if Negative is clear (plus)
		ins.put("BPL", new Instruction() {
			public void ope(int i) {
				if(!NegativeFlag)
					pc=i;
			}
		});
		// break
		ins.put("BRK", new Instruction() {
			public void ope(int i) {
				push(saveStatus());
				pushAddress(pc);
				BreakCommand = true;
				jump(read16bit(0xfffe));
			}
		});
		// branch if OverFlow is clear
		ins.put("BVC", new Instruction() {
			public void ope(int i) {
				if(!OverFlowFlag)
					pc=i;
			}
		});
		// branch if OverFLow is set
		ins.put("BVS", new Instruction() {
			public void ope(int i) {
				if(OverFlowFlag)
					pc=i;
			}
		});
		// clear Carry
		ins.put("CLC", new Instruction() {
			public void ope(int i) {
				CarryFlag = false;
			}
		});
		// clear Decimal
		ins.put("CLD", new Instruction() {
			public void ope(int i) {
				DecimalMode = false;
			}
		});
		// clear Interrupt
		ins.put("CLI", new Instruction() {
			public void ope(int i) {
				InterruptFlag = false;
			}
		});
		// clear OverFlow
		ins.put("CLV", new Instruction() {
			public void ope(int i) {
				OverFlowFlag = false;
			}
		});
		// compare with A
		ins.put("CMP", new Instruction() {
			public void ope(int i) {
				int temp0 = mem.read(i);
				int temp1 = a;
				CarryFlag = (temp1 >= temp0);
				ZeroFlag = (temp1 == temp0);
				NegativeFlag = bit(temp1-temp0,7)==1;
			}
		});
		// compare with X
		ins.put("CPX", new Instruction() {
			public void ope(int i) {
				int temp0 = mem.read(i);
				int temp1 = x;
				CarryFlag = (temp1 >= temp0);
				ZeroFlag = (temp1 == temp0);
				NegativeFlag = bit(temp1-temp0,7)==1;
			}
		});
		// compare with Y
		ins.put("CPY", new Instruction() {
			public void ope(int i) {
				int temp0 = mem.read(i);
				int temp1 = y;
				CarryFlag = (temp1 >= temp0);
				ZeroFlag = (temp1 == temp0);
				NegativeFlag = bit(temp1-temp0,7)==1;
			}
		});
		// decrement
		ins.put("DEC", new Instruction() {
			public void ope(int i) {
				int temp = (mem.read(i)-1)&0xFF;
				mem.write(i, (byte)temp);
				ZeroFlag = (temp == 0);
				NegativeFlag = bit(temp,7) == 1;
			}
		});
		// decrement X
		ins.put("DEX", new Instruction() {
			public void ope(int i) {
				x = (x-1)&0xff;
				ZeroFlag = (x == 0);
				NegativeFlag = bit(x,7) == 1;
			}
		});
		//decrement Y
		ins.put("DEY", new Instruction() {
			public void ope(int i) {
				y = (y-1)&0xff;
				ZeroFlag = (y == 0);
				NegativeFlag = bit(y,7) == 1;
			}
		});
		// or
		ins.put("EOR", new Instruction() {
			public void ope(int i) {
				a = 0xff&(a^(0xff&(mem.read(i))));
				ZeroFlag = (a==0);
				NegativeFlag = bit(a,7) == 1;
			}
		});
		// increment
		ins.put("INC", new Instruction() {
			public void ope(int i) {
				int temp = ((mem.read(i))+1)&0xff;
				mem.write(i, (byte)temp);
				ZeroFlag = (temp == 0);
				NegativeFlag = bit(temp,7) == 1;
			}
		});
		// increment X
		ins.put("INX", new Instruction() {
			public void ope(int i) {
				x = (x+1)&0xff;
				ZeroFlag = (x == 0);
				NegativeFlag = bit(x,7) == 1;
			}
		});
		// increment Y
		ins.put("INY", new Instruction() {
			public void ope(int i) {
				y = (y+1)&0xff;
				ZeroFlag = (y == 0);
				NegativeFlag = bit(y,7) == 1;
			}
		});
		// jump
		ins.put("JMP", new Instruction() {
			public void ope(int i) {
				jump(i);
			}
		});
		// jump to subroutine
		ins.put("JSR", new Instruction() {
			public void ope(int i) {
				pushAddress(pc+2);//push return address-1
				jump(i);
			}
		});
		// load to A
		ins.put("LDA", new Instruction() {
			public void ope(int i) {
				a = mem.read(i);
				ZeroFlag = (a==0);
				NegativeFlag = bit(a,7) == 1;
			}
		});
		// load to X
		ins.put("LDX", new Instruction() {
			public void ope(int i) {
				x = mem.read(i);
				ZeroFlag = (x==0);
				NegativeFlag = bit(x,7) == 1;
			}
		});
		// load to Y
		ins.put("LDY", new Instruction() {
			public void ope(int i) {
				y = mem.read(i);
				ZeroFlag = (y==0);
				NegativeFlag = bit(y,7) == 1;
			}
		});
		// logical bit shift right
		ins.put("LSR", new Instruction() {
			public void ope(int i) {
				int data = (0xff&mem.read(i));
				CarryFlag = (data&1) == 1;
				int temp = data/2;
				mem.write(i, (byte)temp);
				ZeroFlag = (temp&0xff) == 0;
				NegativeFlag = false;
			}
			public void opeAccumulator(){
				int temp = a/2;
				CarryFlag = (a&1) == 1;
				a = temp&0xff;
				ZeroFlag = a == 0;
				NegativeFlag = false;
			}
		});
		// no operation
		ins.put("NOP", new Instruction() {
			public void ope(int i) {
				//do nothing
			}
		});
		// or A
		ins.put("ORA", new Instruction() {
			public void ope(int i) {
				a = a|mem.read(i);
				ZeroFlag = (a==0);
				NegativeFlag = bit(a,7) == 1;
			}
		});
		// push A
		ins.put("PHA", new Instruction() {
			public void ope(int i) {
				push((byte)a);
			}
		});
		// push Status to stack
		ins.put("PHP", new Instruction() {
			public void ope(int i) {
				push(saveStatus());
			}
		});
		// pull from stack to A
		ins.put("PLA", new Instruction() {
			public void ope(int i) {
				a = pull();
				ZeroFlag = (a==0);
				NegativeFlag = bit(a,7) == 1;				
			}
		});
		// store Status from stack
		ins.put("PLP", new Instruction() {
			public void ope(int i) {
				storeStatus(pull());
			}
		});
		// rotate one bit left
		ins.put("ROL", new Instruction() {
			public void ope(int i) {
				int temp = mem.read(i)*2;
				byte res = (byte)(temp | (CarryFlag?1:0));
				mem.write(i, (byte)res);
				CarryFlag = bit(temp,8) == 1;
				ZeroFlag = res == 0;
				NegativeFlag = bit(res,7) == 1;
			}
			public void opeAccumulator(){
				int temp = a*2;
				a = (temp&0xff) | (CarryFlag?1:0);
				CarryFlag = bit(temp,8) == 1;
				ZeroFlag = a == 0;
				NegativeFlag = bit(a,7) == 1;
			}
		});
		// rotate one bit right
		ins.put("ROR", new Instruction() {
			public void ope(int i) {
				int data = mem.read(i);
				int temp = (data/2) | (CarryFlag?0x80:0);
				NegativeFlag = CarryFlag;
				CarryFlag = (data&1) == 1;
				mem.write(i, (byte)temp);
				ZeroFlag = (temp&0xff) == 0;
			}
			public void opeAccumulator(){
				int temp = (a/2) | (CarryFlag?0x80:0);
				NegativeFlag = CarryFlag;
				CarryFlag = (a&1) == 1;
				a = temp&0xff;
				ZeroFlag = a == 0;
			}
		});
		// return from interrupt
		ins.put("RTI", new Instruction() {
			public void ope(int i) {
				nmiOccuring = false;
				storeStatus(pull());
				int r = pullAddress();
				jump(r);
			}
		});
		// return from subroutine
		ins.put("RTS", new Instruction() {
			public void ope(int i) {
				int r = pullAddress();
				jump(r+1);
			}
		});
		// subtract with carry
		ins.put("SBC", new Instruction() {
			public void ope(int i) {
				int data = mem.read(i);
				int temp0 = (0xff&(0xff^data)) + (CarryFlag?1:0);
				int temp = a + temp0;
				int temps = (byte)a + (byte)temp0;
				a = temp & 0xff;
				CarryFlag = (bit(temp,8) == 1);
				ZeroFlag = (a==0);
				OverFlowFlag = temps<-128||127<temps;
				NegativeFlag = bit(temp,7) == 1;
			}
		});
		// set Carry
		ins.put("SEC", new Instruction() {
			public void ope(int i) {
				CarryFlag = true;
			}
		});
		// set Decimal
		ins.put("SED", new Instruction() {
			public void ope(int i) {
				DecimalMode = true;
			}
		});
		// set Interrupt
		ins.put("SEI", new Instruction() {
			public void ope(int i) {
				InterruptFlag = true;
			}
		});
		// store A
		ins.put("STA", new Instruction() {
			public void ope(int i) {
				mem.write(i, (byte)a);
			}
		});
		// store X
		ins.put("STX", new Instruction() {
			public void ope(int i) {
				mem.write(i, (byte)x);
			}
		});
		// store Y
		ins.put("STY", new Instruction() {
			public void ope(int i) {
				mem.write(i, (byte)y);
			}
		});
		// transfer A to X
		ins.put("TAX", new Instruction() {
			public void ope(int i) {
				x = a;
				ZeroFlag = (x==0);
				NegativeFlag = bit(x,7) == 1;
			}
		});
		// transfer A to Y
		ins.put("TAY", new Instruction() {
			public void ope(int i) {
				y = a;
				ZeroFlag = (y==0);
				NegativeFlag = bit(y,7) == 1;
			}
		});
		// transfer X to A
		ins.put("TXA", new Instruction() {
			public void ope(int i) {
				a = x;
				ZeroFlag = (a==0);
				NegativeFlag = bit(a,7) == 1;
			}
		});
		// transfer Y to A
		ins.put("TYA", new Instruction() {
			public void ope(int i) {
				a = y;
				ZeroFlag = (a==0);
				NegativeFlag = bit(a,7) == 1;
			}
		});
		// transfer S to X
		ins.put("TSX", new Instruction() {
			public void ope(int i) {
				x = s;
				ZeroFlag = (x==0);
				NegativeFlag = bit(x,7) == 1;
			}
		});
		// transfer X to S
		ins.put("TXS", new Instruction() {
			public void ope(int i) {
				s = x;
			}
		});
	}
	
	Operand[] operands = new Operand[256];
	private void setop(int op, String add, String ope, int clc){
		operands[op] = new Operand(op, add, ope, clc);
	}
	{
		setop(0xA9,"LDA","Immediate", 2);
		setop(0xA5,"LDA","ZeroPage", 3);
		setop(0xB5,"LDA","ZeroPageX", 4);
		setop(0xAD,"LDA","Absolute", 4);
		setop(0xBD,"LDA","AbsoluteX", 4);
		setop(0xB9,"LDA","AbsoluteY", 4);
		setop(0xA1,"LDA","IndirectX", 6);
		setop(0xB1,"LDA","IndirectY", 5);
		setop(0xA2,"LDX","Immediate", 2);
		setop(0xA6,"LDX","ZeroPage", 3);
		setop(0xB6,"LDX","ZeroPageY", 4);
		setop(0xAE,"LDX","Absolute", 4);
		setop(0xBE,"LDX","AbsoluteY", 4);
		setop(0xA0,"LDY","Immediate", 2);
		setop(0xA4,"LDY","ZeroPage", 3);
		setop(0xB4,"LDY","ZeroPageX", 4);
		setop(0xAC,"LDY","Absolute", 4);
		setop(0xBC,"LDY","AbsoluteX", 4);
		setop(0x85,"STA","ZeroPage", 3);
		setop(0x95,"STA","ZeroPageX", 4);
		setop(0x8D,"STA","Absolute", 4);
		setop(0x9D,"STA","AbsoluteX", 5);
		setop(0x99,"STA","AbsoluteY", 5);
		setop(0x81,"STA","IndirectX", 6);
		setop(0x91,"STA","IndirectY", 6);
		setop(0x86,"STX","ZeroPage", 3);
		setop(0x96,"STX","ZeroPageY", 4);
		setop(0x8E,"STX","Absolute", 4);
		setop(0x84,"STY","ZeroPage", 3);
		setop(0x94,"STY","ZeroPageX", 4);
		setop(0x8C,"STY","Absolute", 4);
		setop(0xAA,"TAX","Implicit", 2);
		setop(0xA8,"TAY","Implicit", 2);
		setop(0x8A,"TXA","Implicit", 2);
		setop(0x98,"TYA","Implicit", 2);
		setop(0xBA,"TSX","Implicit", 2);
		setop(0x9A,"TXS","Implicit", 2);
		setop(0x48,"PHA","Implicit", 3);
		setop(0x08,"PHP","Implicit", 3);
		setop(0x68,"PLA","Implicit", 4);
		setop(0x28,"PLP","Implicit", 4);
		setop(0x29,"AND","Immediate", 2);
		setop(0x25,"AND","ZeroPage", 3);
		setop(0x35,"AND","ZeroPageX", 4);
		setop(0x2D,"AND","Absolute", 4);
		setop(0x3D,"AND","AbsoluteX", 4);
		setop(0x39,"AND","AbsoluteY", 4);
		setop(0x21,"AND","IndirectX", 6);
		setop(0x31,"AND","IndirectY", 5);
		setop(0x49,"EOR","Immediate", 2);
		setop(0x45,"EOR","ZeroPage", 3);
		setop(0x55,"EOR","ZeroPageX", 4);
		setop(0x4D,"EOR","Absolute", 4);
		setop(0x5D,"EOR","AbsoluteX", 4);
		setop(0x59,"EOR","AbsoluteY", 4);
		setop(0x41,"EOR","IndirectX", 6);
		setop(0x51,"EOR","IndirectY", 5);
		setop(0x09,"ORA","Immediate", 2);
		setop(0x05,"ORA","ZeroPage", 3);
		setop(0x15,"ORA","ZeroPageX", 4);
		setop(0x0D,"ORA","Absolute", 4);
		setop(0x1D,"ORA","AbsoluteX", 4);
		setop(0x19,"ORA","AbsoluteY", 4);
		setop(0x01,"ORA","IndirectX", 6);
		setop(0x11,"ORA","IndirectY", 5);
		setop(0x24,"BIT","ZeroPage", 3);
		setop(0x2C,"BIT","Absolute", 4);
		setop(0x69,"ADC","Immediate", 2);
		setop(0x65,"ADC","ZeroPage", 3);
		setop(0x75,"ADC","ZeroPageX", 4);
		setop(0x6D,"ADC","Absolute", 4);
		setop(0x7D,"ADC","AbsoluteX", 4);
		setop(0x79,"ADC","AbsoluteY", 4);
		setop(0x61,"ADC","IndirectX", 6);
		setop(0x71,"ADC","IndirectY", 5);
		setop(0xE9,"SBC","Immediate", 2);
		setop(0xE5,"SBC","ZeroPage", 3);
		setop(0xF5,"SBC","ZeroPageX", 4);
		setop(0xED,"SBC","Absolute", 4);
		setop(0xFD,"SBC","AbsoluteX", 4);
		setop(0xF9,"SBC","AbsoluteY", 4);
		setop(0xE1,"SBC","IndirectX", 6);
		setop(0xF1,"SBC","IndirectY", 5);
		setop(0xC9,"CMP","Immediate", 2);
		setop(0xC5,"CMP","ZeroPage", 3);
		setop(0xD5,"CMP","ZeroPageX", 4);
		setop(0xCD,"CMP","Absolute", 4);
		setop(0xDD,"CMP","AbsoluteX", 4);
		setop(0xD9,"CMP","AbsoluteY", 4);
		setop(0xC1,"CMP","IndirectX", 6);
		setop(0xD1,"CMP","IndirectY", 5);
		setop(0xE0,"CPX","Immediate", 2);
		setop(0xE4,"CPX","ZeroPage", 3);
		setop(0xEC,"CPX","Absolute", 4);
		setop(0xC0,"CPY","Immediate", 2);
		setop(0xC4,"CPY","ZeroPage", 3);
		setop(0xCC,"CPY","Absolute", 4);
		setop(0xE6,"INC","ZeroPage", 5);
		setop(0xF6,"INC","ZeroPageX", 6);
		setop(0xEE,"INC","Absolute", 6);
		setop(0xFE,"INC","AbsoluteX", 7);
		setop(0xE8,"INX","Implicit", 2);
		setop(0xC8,"INY","Implicit", 2);
		setop(0xC6,"DEC","ZeroPage", 5);
		setop(0xD6,"DEC","ZeroPageX", 6);
		setop(0xCE,"DEC","Absolute", 6);
		setop(0xDE,"DEC","AbsoluteX", 7);
		setop(0xCA,"DEX","Implicit", 2);
		setop(0x88,"DEY","Implicit", 2);
		setop(0x0A,"ASL","Accumulator", 2);
		setop(0x06,"ASL","ZeroPage", 5);
		setop(0x16,"ASL","ZeroPageX", 6);
		setop(0x0E,"ASL","Absolute", 6);
		setop(0x1E,"ASL","AbsoluteX", 7);
		setop(0x4A,"LSR","Accumulator", 2);
		setop(0x46,"LSR","ZeroPage", 5);
		setop(0x56,"LSR","ZeroPageX", 6);
		setop(0x4E,"LSR","Absolute", 6);
		setop(0x5E,"LSR","AbsoluteX", 7);
		setop(0x2A,"ROL","Accumulator", 2);
		setop(0x26,"ROL","ZeroPage", 5);
		setop(0x36,"ROL","ZeroPageX", 6);
		setop(0x2E,"ROL","Absolute", 6);
		setop(0x3E,"ROL","AbsoluteX", 7);
		setop(0x6A,"ROR","Accumulator", 2);
		setop(0x66,"ROR","ZeroPage", 5);
		setop(0x76,"ROR","ZeroPageX", 6);
		setop(0x6E,"ROR","Absolute", 6);
		setop(0x7E,"ROR","AbsoluteX", 7);
		setop(0x4C,"JMP","Absolute", 3);
		setop(0x6C,"JMP","Indirect", 5);
		setop(0x20,"JSR","Absolute", 6);
		setop(0x60,"RTS","Implicit", 6);
		setop(0x90,"BCC","Relative", 2);
		setop(0xB0,"BCS","Relative", 2);
		setop(0xF0,"BEQ","Relative", 2);
		setop(0x30,"BMI","Relative", 2);
		setop(0xD0,"BNE","Relative", 2);
		setop(0x10,"BPL","Relative", 2);
		setop(0x50,"BVC","Relative", 2);
		setop(0x70,"BVS","Relative", 2);
		setop(0x18,"CLC","Implicit", 2);
		setop(0xD8,"CLD","Implicit", 2);
		setop(0x58,"CLI","Implicit", 2);
		setop(0xB8,"CLV","Implicit", 2);
		setop(0x38,"SEC","Implicit", 2);
		setop(0xF8,"SED","Implicit", 2);
		setop(0x78,"SEI","Implicit", 2);
		setop(0x00,"BRK","Implicit", 7);
		setop(0xEA,"NOP","Implicit", 2);
		setop(0x1A,"NOP","Implicit", 2);
		setop(0x3A,"NOP","Implicit", 2);
		setop(0x5A,"NOP","Implicit", 2);
		setop(0x7A,"NOP","Implicit", 2);
		setop(0xDA,"NOP","Implicit", 2);
		setop(0xFA,"NOP","Implicit", 2);
		setop(0x04,"NOP","Immediate", 2);
		setop(0x44,"NOP","Immediate", 2);
		setop(0x64,"NOP","Immediate", 2);
		setop(0x14,"NOP","IndirectX", 2);
		setop(0x34,"NOP","IndirectX", 2);
		setop(0x54,"NOP","IndirectX", 2);
		setop(0x74,"NOP","IndirectX", 2);
		setop(0xD4,"NOP","IndirectX", 2);
		setop(0xF4,"NOP","IndirectX", 2);
		setop(0x80,"NOP","ZeroPage", 2);// TODO
		setop(0x0C,"NOP","Absolute", 2);
		setop(0x1C,"NOP","AbsoluteX", 2);
		setop(0x3C,"NOP","AbsoluteX", 2);
		setop(0x5C,"NOP","AbsoluteX", 2);
		setop(0x7C,"NOP","AbsoluteX", 2);
		setop(0xDC,"NOP","AbsoluteX", 2);
		setop(0xFC,"NOP","AbsoluteX", 2);
		setop(0x40,"RTI","Implicit", 6);
	}
	
	int exec(){
		if(nmi && !nmiOccuring){
			nmiOccuring = true;
			nmi = false;
//			Debug.println("NMI occurred.");
			pushAddress(pc);
			push(saveStatus());
			BreakCommand = true;			
			pc = (0xff&mem.read(0xfffa)) | ((mem.read(0xfffb)&0xff)<<8);
		}

		if(debugger != null) debugger.debug(this);//DEBUG

		int op = mem.read(pc);
		Operand ope = operands[op];
		Debug.print(Debug.hex4(pc)+" ");
		Debug.print("A:"+Debug.hex4(a)+" X:"+Debug.hex2(x)+" Y:"+Debug.hex2(y)+" S:"+Debug.hex2(s)+" ST:"+statusStr());
		Debug.println(" "+ope.instStr + " " + ope.addr.toString(mem.data[pc+1], mem.data[pc+2]));
		ope.addr.ope(ope.inst);
		this.clock = (this.clock + ope.clock)%CLOCK_SPEED;
		
		return ope.clock;
	}
	
	void reset(){
		pc = (0xff&mem.read(0xfffc)) | ((mem.read(0xfffd)&0xff)<<8);
		a = x = y = 0;
		s = 0xff;
	}
	
	boolean nmi = false;
	void setNMI(){
		nmi = true;
	}
	
	CPUDebugger debugger = new CPUDebugger();
	
	Operand dbgToOpe(int addr){
		return operands[0xff&mem.data[addr]];
	}
	byte[] dbgFetch(int addr, int size){
		byte[] res = new byte[size];
		for(int i=0; i<size; i++)
			res[i] = mem.data[addr+i];
		return res;
	}

	String statusStr(){
		return
			(NegativeFlag?"N":"_") +
			(OverFlowFlag?"O":"_") + "__" +
			(DecimalMode?"D":"_") +
			(InterruptFlag?"I":"_") +
			(ZeroFlag?"Z":"_") +
			(CarryFlag?"C":"_");
	}
}
