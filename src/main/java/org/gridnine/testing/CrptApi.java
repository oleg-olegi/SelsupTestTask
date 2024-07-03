package org.gridnine.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private final HttpClient client;
    private final ObjectMapper objectMapper;
    private final Semaphore semaphore;
    private final long timeUnitMillis;
    private final boolean useMock;

    public CrptApi(TimeUnit timeUnit, int requestLimit, boolean useMock) {
        this.useMock = useMock;
        this.client = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.semaphore = new Semaphore(requestLimit);
        this.timeUnitMillis = timeUnit.toMillis(1);

        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(timeUnitMillis);
                    semaphore.release(requestLimit - semaphore.availablePermits());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

    public void createDocument(Document document, String signature) throws IOException, InterruptedException {
        semaphore.acquire();

        if (useMock) {
            // мок-ответ
            System.out.println("Mock response: Document created successfully.");
            return;
        }

        ObjectNode json = objectMapper.createObjectNode();
        json.set("description", objectMapper.valueToTree(document));
        json.put("signature", signature);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(json)))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to create document: " + response.body());
        }
        System.out.println("Response: " + response.body());
    }

    public static class Document {
        public String participantInn;
        public String docId;
        public String docStatus;
        public String docType;
        public boolean importRequest;
        public String ownerInn;
        public String participantInnInner;
        public String producerInn;
        public String productionDate;
        public String productionType;
        public Product[] products;
        public String regDate;
        public String regNumber;

        public static class Product {
            public String certificateDocument;
            public String certificateDocumentDate;
            public String certificateDocumentNumber;
            public String ownerInn;
            public String producerInn;
            public String productionDate;
            public String tnvedCode;
            public String uitCode;
            public String uituCode;
        }
    }

    public static void main(String[] args) throws InterruptedException, IOException {

        CrptApi api = new CrptApi(TimeUnit.SECONDS, 10, true);

        Document doc = new Document();

        doc.participantInn = "1234567890";
        doc.docId = "docId";
        doc.docStatus = "status";
        doc.docType = "LP_INTRODUCE_GOODS";
        doc.importRequest = true;
        doc.ownerInn = "ownerInn";
        doc.participantInnInner = "participantInnInner";
        doc.producerInn = "producerInn";
        doc.productionDate = "2020-01-23";
        doc.productionType = "productionType";
        doc.products = new Document.Product[1];
        doc.products[0] = new Document.Product();
        doc.products[0].certificateDocument = "certDoc";
        doc.products[0].certificateDocumentDate = "2020-01-23";
        doc.products[0].certificateDocumentNumber = "certDocNum";
        doc.products[0].ownerInn = "ownerInn";
        doc.products[0].producerInn = "producerInn";
        doc.products[0].productionDate = "2020-01-23";
        doc.products[0].tnvedCode = "tnvedCode";
        doc.products[0].uitCode = "uitCode";
        doc.products[0].uituCode = "uituCode";
        doc.regDate = "2020-01-23";
        doc.regNumber = "regNumber";

        api.createDocument(doc, "подпись");
    }
}

