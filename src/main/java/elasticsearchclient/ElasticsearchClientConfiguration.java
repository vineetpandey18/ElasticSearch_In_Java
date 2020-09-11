package elasticsearchclient;

import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig.Builder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder.RequestConfigCallback;

@Configuration
public class ElasticsearchClientConfiguration {
    @Bean(destroyMethod = "close")
    RestClient transportClient() {
        return RestClient
            .builder(new HttpHost("localhost", 9200))
            .setRequestConfigCallback(new RequestConfigCallback() {
                 // @Override
//                  public Builder customizeRequestConfig(Builder builder) {
//                      return builder
//                          .setConnectTimeout(1000)
//                          .setSocketTimeout(5000);
//                  }

				public Builder customizeRequestConfig(Builder requestConfigBuilder) {
					return requestConfigBuilder
	                          .setConnectTimeout(1000)
	                          .setSocketTimeout(5000);
				}
            })
            .build();
    }
}
