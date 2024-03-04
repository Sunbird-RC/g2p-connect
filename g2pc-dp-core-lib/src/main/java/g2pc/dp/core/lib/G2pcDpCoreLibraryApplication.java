package g2pc.dp.core.lib;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })
@ComponentScan({"g2pc.core.lib","g2pc.dp.core.lib","g2pc.dp.core.lib.serviceimpl"})
public class G2pcDpCoreLibraryApplication {

	public static void main(String[] args) {
		SpringApplication.run(G2pcDpCoreLibraryApplication.class, args);
	}

}