package edu.sjsu.cmpe.library;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.assets.AssetsBundle;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.views.ViewBundle;

import edu.sjsu.cmpe.library.api.resources.BookResource;
import edu.sjsu.cmpe.library.api.resources.RootResource;
import edu.sjsu.cmpe.library.config.LibraryServiceConfiguration;
import edu.sjsu.cmpe.library.repository.BookRepository;
import edu.sjsu.cmpe.library.repository.BookRepositoryInterface;
import edu.sjsu.cmpe.library.ui.resources.HomeResource;

public class LibraryService extends Service<LibraryServiceConfiguration> {

    private final Logger log = LoggerFactory.getLogger(getClass());
	private static Connection connection;
    private static MessageProducer producer;
    private static String instanceName = "";

    public static void main(String[] args) throws Exception {
	new LibraryService().run(args);
	ExecutorService execution = Executors.newFixedThreadPool(numThreads);
	  
	 Runnable backTask = new Runnable() {
	  
	 @Override
	 public void run() {
	 try{
		ListenToApolloTopic();
	 }
	 catch(JMSException e){
		 System.out.println("Exception :  " + e.getMessage());
	 }
	 }
	  executor.execution(backTask);
	 executor.shutdown();
    }

    @Override
    public void initialize(Bootstrap<LibraryServiceConfiguration> bootstrap) {
	bootstrap.setName("library-service");
	//old
		bootstrap.addBundle(new ViewBundle());
 //new
	bootstrap.addBundle(new AssetsBundle());

    }

    @Override
    public void run(LibraryServiceConfiguration configuration,
	    Environment environment) throws Exception {
	// This is how you pull the configurations from library_x_config.yml
	String queueName = configuration.getStompQueueName();
	String topicName = configuration.getStompTopicName();
	log.debug("Queue name is {}. Topic name is {}", queueName,
		topicName);
	// TODO: Apollo STOMP Broker URL and login
	public static void OrderForNewBook(Long lostisbn)throws JMSException{
    	
    	String queue = "/queue/30765.book.orders";
	 	
    	StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
    	factory.setBrokerURI("tcp://" + host + ":" + port);
    	connection = factory.createConnection(user, password);
    	connection.start();
    	
    	Destination dest = new StompJmsDestination(queue);
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
       
        producer = session.createProducer(dest);
        producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        
        String data = instanceName+":"+lostisbn.toString();
        TextMessage msg = session.createTextMessage(data);
        producer.send(msg);
        
        connection.close();
    }

	/** Root API */
	environment.addResource(RootResource.class);
	/** Books APIs */
	BookRepositoryInterface bookRepository = new BookRepository();
	environment.addResource(new BookResource(bookRepository));

	/** UI Resources */
	environment.addResource(new HomeResource(bookRepository));
	 public static void ListenToApolloTopic() throws JMSException{
    	
    	 String destination = "";
    	 if(instanceName != "" && instanceName.equalsIgnoreCase("library-a"))
    		 destination = "/topic/30765.book.*";
    	 else 
    		 destination = "/topic/30765.book.computer";
    	 
    	 
         StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
         factory.setBrokerURI("tcp://" + host + ":" + port);

         Connection connection = factory.createConnection(user, password);
         connection.start();
         
         Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
         Destination dest = new StompJmsDestination(destination);

         MessageConsumer consumer = session.createConsumer(dest);
         
         while(true) {
          Message msg = consumer.receive();
          if(msg == null)
        	  continue;
          if( msg instanceof TextMessage ) {
                 String body = ((TextMessage) msg).getText();                 
                 if( "SHUTDOWN".equals(body)) {
                  continue;
                 }
                 BookRepository.UpdateBookDetails(body);
          } else if (msg instanceof StompJmsMessage) {
                 StompJmsMessage smsg = ((StompJmsMessage) msg);
                 String body = smsg.getFrame().contentAsString();                 
                 if ("SHUTDOWN".equals(body)) {
                	 continue;
                 }
                 BookRepository.UpdateBookDetails(body);
          } else {
                 continue;
          }
         }
    }
}
