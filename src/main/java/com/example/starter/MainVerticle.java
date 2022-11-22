package com.example.starter;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.micrometer.PrometheusScrapingHandler;
import io.vertx.micrometer.backends.BackendRegistries;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;

public class MainVerticle extends AbstractVerticle {

//	public static void main(String[] args) {
//		VertxOptions vertxO =  new VertxOptions().setMetricsOptions(new MicrometerMetricsOptions()
//				.setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true)).setEnabled(true));
//		System.out.println(vertxO.toJson());
//	}

	@Override
	public void start(Promise<Void> startPromise) throws Exception {

		MeterRegistry registry = BackendRegistries.getDefaultNow();

		new ClassLoaderMetrics().bindTo(registry);
		new ProcessorMetrics().bindTo(registry);
		new JvmMemoryMetrics().bindTo(registry);
		new JvmThreadMetrics().bindTo(registry);

		Router router = Router.router(vertx);
		router.route("/metrics").handler(PrometheusScrapingHandler.create());
		vertx.createHttpServer().requestHandler(router).listen(config().getInteger("http.port", 8080));

		MqttClientOptions options = new MqttClientOptions();
		options.setCleanSession(false);
		options.setClientId("2b5cf8d8-7d04-4516-8e9d-2ed7fb9a2eb0");
		MqttClient client = MqttClient.create(vertx, options);

		client.connect(1883, "localhost").andThen(ret -> {

			client.publishHandlerManualAck(msg -> {

				vertx.executeBlocking(promise -> {

					System.out.println(Thread.currentThread().getName() + " New message in topic: " + msg.topicName()
							+ " qos: " + msg.qosLevel() + " content: " + msg.payload().toString());

					try {
						Double d = Math.random() * 2500;
						Thread.sleep(d.longValue());
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					System.out.println(Thread.currentThread().getName() + " Done message: " + msg.payload().toString());
					msg.ack();

				}, false);

			}).subscribe("demo", 2);

		});

	}
}
