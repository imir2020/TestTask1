
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class CrptApi {
    private final Semaphore requestSemaphore;
    private final Lock lock;
    private final TimeUnit timeUnit;
    private final int requestLimit;
    public static final int TIME_INTERVAL = 1;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.requestSemaphore = new Semaphore(requestLimit);
        this.lock = new ReentrantLock();

    }

    /**
     * Вспомогательный метод, возвращает будущую дату, в секундах.
     *
     * @return
     */
    private long convertTime() {
        return Instant.EPOCH.getEpochSecond() + TimeUnit.SECONDS.convert(TIME_INTERVAL, timeUnit);
    }


    /**
     * Производит заданное количество запросов, за заданный промежуток времени.
     * @param document
     * @param signature
     * @throws IOException
     */
    public void createDocumentForGoodsIntroduction(JsonObject document, String signature) throws IOException {
        long time = convertTime();
        lock.lock();
        try {
            if (!requestSemaphore.tryAcquire()) {
                if (Instant.EPOCH.getEpochSecond() <= time) {
                    requestSemaphore.release(requestLimit);
                } else {
                    Thread.sleep(1, 0);
                    return;
                }
            }

            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpPost httpPost = new HttpPost("https://ismp.crpt.ru/api/v3/lk/documents/create");
            httpPost.setEntity(new StringEntity(document.toString()));
            httpPost.setHeader("Content-Type", "application/json");

            httpClient.execute(httpPost);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            requestSemaphore.release();
            lock.unlock();
        }
    }

    public static void main(String[] args) throws IOException {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 10);
        JsonObject document = new JsonObject();
        document.addProperty("participantInn", "string");
        crptApi.createDocumentForGoodsIntroduction(document, "signature");

    }
}
