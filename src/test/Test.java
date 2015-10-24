package test;

public class Test {
	public static void main(String[] args) {
		for(int i=-500; i<500; i++){
			System.out.print(i+" ");
			System.out.print((byte)i+" ");
			System.out.print((byte)(i&0xff)+" ");
			System.out.print((byte)(int)(byte)i+" ");
			System.out.println();
		}
	}
}
