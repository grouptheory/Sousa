import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.net.ServerSocket;
import java.net.Socket;

public class StreamTokenPrinter {

	public static void main(String args[]) {
		try {
			ServerSocket server = new ServerSocket(Integer.parseInt(args[0]));

			Socket sock = server.accept();
			StreamTokenizer tokenizer = 
				new StreamTokenizer(new BufferedReader(new InputStreamReader(sock.getInputStream())));
			int type;
			while (StreamTokenizer.TT_EOF != tokenizer.nextToken()) {

				switch (tokenizer.ttype) {
				case StreamTokenizer.TT_EOF:
					System.out.println("TT_EOF");
					break;

				case StreamTokenizer.TT_EOL:
					System.out.println("TT_EOL");
					break;

				case StreamTokenizer.TT_NUMBER:
					System.out.println("TT_NUMBER: " + tokenizer.nval);
					break;

				case StreamTokenizer.TT_WORD:
					System.out.println("TT_WORD: " + tokenizer.sval);
				}
			}

			System.out.println("TT_EOF");
		}
		catch (IOException ex) {
			System.out.println(ex);
		}
		catch (NumberFormatException ex) {
			System.out.println(ex);
		}
	}
}