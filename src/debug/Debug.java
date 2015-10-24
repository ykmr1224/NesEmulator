package debug;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

public class Debug {
	static PrintWriter out;
	static{
		try {
			out = new PrintWriter(new FileOutputStream(new File("out.txt")));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public static void println(String str){
		out.println(str);
	}
	public static void print(String str){
		out.print(str);
	}

	static char[] h = new char[]{
			'0', '1', '2', '3',
			'4', '5', '6', '7',
			'8', '9', 'A', 'B',
			'C', 'D', 'E', 'F',
	};
	public static String hex2(int i){
		i = i&0xff;
		StringBuffer buff = new StringBuffer();
		buff.append(h[(i/16)%16]);
		buff.append(h[i%16]);
		return buff.toString();
	}
	public static String hex4(int i){
		return hex2(i>>8)+hex2(i);
	}
	public static String bit8(int i){
		StringBuffer buff = new StringBuffer(8);
		for(int j=7; j>=0; j--){
			buff.append(1&(i>>j));
		}
		return buff.toString();
	}
}
