package transferecc;

/**
 *
 * @author alvesluis
 */
public class Cliente {
    public static void main(String[] args) {
        if (args.length < 1)
            System.out.println(Menu.insuficientArguments(args.length));
        else {
            Configuration config = new Configuration(args[0]);
        
            System.out.println(Menu.welcomeMenu(config.getIPS()));
        }
    }
}
