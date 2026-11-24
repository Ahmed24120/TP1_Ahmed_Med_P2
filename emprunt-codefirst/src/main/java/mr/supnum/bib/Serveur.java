package mr.supnum.bib;

import jakarta.xml.ws.Endpoint;

public class Serveur {
    public static void main(String[] args) throws InterruptedException {
        String url = "http://localhost:8080/bib/EmpruntService";
        Endpoint endpoint = Endpoint.publish(url, new EmpruntService());
        Runtime.getRuntime().addShutdownHook(new Thread(endpoint::stop));
        System.out.println("WSDL : " + url + "?wsdl");
        new java.util.concurrent.CountDownLatch(1).await();
    }
}
