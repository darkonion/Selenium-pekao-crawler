package crawler;

import java.util.Map;


public class TestClass {

    public static void main(String[] args) {

        //it needs some validation
        System.out.println("Enter ClientId: ");
        String clientId = System.console().readLine();

        //it needs some validation
        System.out.println("Enter Password: ");
        char[] password = System.console().readPassword();

        BankCrawler instance = BankCrawler.getInstance(password, clientId);
        instance.crawl();
        printData(instance.getData());
    }

    private static void printData(Map<String, String> data) {
        System.out.println("\n");
        for(String title : data.keySet()) {
            System.out.println(title + ": " + data.get(title));
        }
    }
}
