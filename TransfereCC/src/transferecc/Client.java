package transferecc;

import java.util.Scanner;

/**
 *
 * @author alvesluis
 */
public class Client {
    public static void main(String[] args) {
        if (args.length < 1)
            System.out.println(Menu.insuficientArguments(args.length));
        else {
            Configuration config = new Configuration(args[0]);
        
            System.out.println(Menu.welcomeMenu(config.getIPS()));
            
            Scanner in = new Scanner(System.in);
            int res=in.nextInt();
            int numIps= config.numIps();
            
            
        }
    }
    
    public nextAction(int acao, Configuration config){
        int nIps= config.numIps();
        Scanner in = new Scanner(System.in);
        if(nIps==acao){
            System.out.println("Please insert the IP.");
            config.addIP(in.next());
            return;
        }
        else{
            if(nIps==acao)
        }
        
    }
}
