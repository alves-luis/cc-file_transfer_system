package app;

/**
 *
 * @author alvesluis
 */
public class Menu {
    
    public static String welcomeMenu(String[] ips) {
        StringBuilder sb = new StringBuilder();
        sb.append("Welcome to TransferCC! \nThis a reliable UDP File Transfer Protocol, brought to you by Luis and Rafaela!\n");
        sb.append("To which server do you wish to connect to?\n");
        sb.append("0) ").append("I want to add a new IP\n");
        for(int i = 1; i <= ips.length;i++) {
            String ip = ips[i-1];
            sb.append(i).append(") ").append(ip).append("\n");
        }
        return sb.toString();
    }

    public static String insuficientArguments(int length) {
        StringBuilder sb = new StringBuilder();
        sb.append("Insuficient number of arguments! (").append(length); 
        sb.append(")\nPlease run the program like this: java Client <PUT/GET> <filename> <serverIP>");
        return sb.toString();
    }
    
   
}
