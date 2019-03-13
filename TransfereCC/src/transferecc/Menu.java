package transferecc;

/**
 *
 * @author alvesluis
 */
public class Menu {
    
    public static String welcomeMenu(String[] ips) {
        StringBuilder sb = new StringBuilder();
        sb.append("Welcome to TransferCC! \nThis a reliable UDP File Transfer Protocol, brought to you by Luis and Rafaela!\n");
        sb.append("To which server do you wish to connect to?\n");
        for(int i = 0; i < ips.length;i++) {
            String ip = ips[i];
            sb.append(i).append(") ").append(ip).append("\n");
        }
        sb.append(ips.length).append(") ").append("I want to add a new IP\n");
        return sb.toString();
    }

    public static String insuficientArguments(int length) {
        StringBuilder sb = new StringBuilder();
        sb.append("Insuficient number of arguments! (").append(length); 
        sb.append(")\nPlease run the program like this: java Cliente <IPsFile>");
        return sb.toString();
    }
    
    
}