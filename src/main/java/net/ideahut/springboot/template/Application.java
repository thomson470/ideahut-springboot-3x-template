package net.ideahut.springboot.template;

import org.hibernate.Version;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.SpringVersion;

import lombok.extern.slf4j.Slf4j;
import net.ideahut.springboot.IdeahutVersion;
import net.ideahut.springboot.admin.AdminHandler;
import net.ideahut.springboot.audit.AuditHandler;
import net.ideahut.springboot.bean.BeanConfigure;
import net.ideahut.springboot.entity.EntityTrxManager;
import net.ideahut.springboot.grid.GridHandler;
import net.ideahut.springboot.init.InitHandler;
import net.ideahut.springboot.task.TaskHandler;
import net.ideahut.springboot.template.service.MessageService;
import net.ideahut.springboot.util.BeanUtil;

@Slf4j
@SpringBootApplication
public class Application extends SpringBootServletInitializer implements ApplicationListener<ContextRefreshedEvent> {
	
	@Autowired
	private AppProperties appProperties;
	@Autowired
	private ServletWebServerApplicationContext webServerApplicationContext;
	
	private static boolean ready = false;
	
	public static boolean isReady() {
		return ready;
	}
	
	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(Application.class);
		application.setLazyInitialization(false);
		application.setLogStartupInfo(true);
		application.run(args);
	}
	
	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(Application.class);
	}
	
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		ready = false;
		ApplicationContext applicationContext = event.getApplicationContext();
		log.info("**** Initializing application '" + appProperties.getInstanceId() + "'");
		TaskHandler commonAsync = applicationContext.getBean(AppConstants.Bean.Async.COMMON, TaskHandler.class);
		commonAsync.execute(() -> {
			BeanConfigure.runBeanConfigure(applicationContext, EntityTrxManager.class, null);
			InitHandler initHandler = applicationContext.getBean(InitHandler.class);
			commonAsync.execute(new Runnable() {
				@Override
				public void run() {
					try {
						initHandler.initClass();
						initHandler.initMapper(applicationContext);
						initHandler.initValidation();
					} catch (Exception e) {
						throw BeanUtil.exception(e);
					}
				}
			});
			BeanConfigure.runBeanConfigure(applicationContext, AuditHandler.class, commonAsync);
			BeanConfigure.runBeanConfigure(applicationContext, GridHandler.class, commonAsync);
			BeanConfigure.runBeanConfigure(applicationContext, AdminHandler.class, commonAsync);
			BeanConfigure.runBeanConfigure(applicationContext, MessageService.class, commonAsync);
			ready = true;
			
			// Inisialisasi Servlet dengan mengirim request ke endpoint /warmup (Lihat WarmUpController)
			// Request dikirim setelah reconfigure selesai
			initHandler.initServlet();
			
			log.info("**** JDK              : " + System.getProperty("java.version"));
			log.info("**** Spring Framework : " + SpringVersion.getVersion());
			log.info("**** Spring Boot      : " + SpringBootVersion.getVersion());
			log.info("**** Hibernate        : " + Version.getVersionString());
			log.info("**** Ideahut          : " + IdeahutVersion.getVersion());
			log.info("**** Application '" + webServerApplicationContext.getId() + "' is ready to serve on port " + webServerApplicationContext.getWebServer().getPort());
			
		});
	}
	
}
