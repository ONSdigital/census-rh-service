package uk.gov.ons.ctp.integration.rhsvc.cloud;

import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.FieldPath;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import uk.gov.ons.ctp.common.event.model.Address;
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.common.event.model.CasePayload;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.Header;


/**
 * A simple Quick start application demonstrating how to connect to Firestore
 * and add and query documents.
 * 
 * PMB - DELETE THIS FILE
 */
public class FirestorePerformanceTester {

  private Firestore db;
  
  public FirestorePerformanceTester() {
    this.db = FirestoreOptions.getDefaultInstance().getService();
  }

  
  private void createCasesAsynchronous(int num) throws Exception {
    long creationStart = System.currentTimeMillis();
    
    ApiFuture<WriteResult> result = null;
    for (int i=0; i<num; i++) {
      CaseEvent caseEvent = createCase(i);
      
      long start = System.currentTimeMillis();
      result = db.collection("cases")
          .document(caseEvent.getEvent().getTransactionId())
          .set(caseEvent);
      long end = System.currentTimeMillis();
      System.out.printf("Adding %5d %dms\n", i, (end-start));
    }
    result.get();
    
    long creationEnd = System.currentTimeMillis();
    System.out.println("Overall creation time:" + (creationEnd-creationStart) + "ms");
  }

  
  private void createCasesSynchronous(int num) throws Exception {
    long creationStart = System.currentTimeMillis();
    
    for (int i=0; i<num; i++) {
      CaseEvent caseEvent = createCase(i);
      
      long start = System.currentTimeMillis();
      ApiFuture<WriteResult> result = db.collection("cases")
          .document(caseEvent.getEvent().getTransactionId())
          .set(caseEvent);
      result.get();
      long end = System.currentTimeMillis();
      
      System.out.printf("Adding %5d %dms %s\n", i, (end-start));
    }
    
    long creationEnd = System.currentTimeMillis();
    System.out.println("Overall creation time:" + (creationEnd-creationStart) + "ms");
  }


  private void randomRead(int max, int numToRead) throws Exception {
    Random rnd = new Random();
    
    for (int i=0; i<numToRead; i++) {
      int uprnNumber = rnd.nextInt(max);
      String uprnString = Integer.toString(uprnNumber);
      
      long start = System.currentTimeMillis();
      FieldPath path2 = FieldPath.of("payload", "collectionCase", "address", "uprn");
      ApiFuture<QuerySnapshot> query = db.collection("cases").whereEqualTo(path2, uprnString).get();

      List<QueryDocumentSnapshot> documents = query.get().getDocuments();
      for (QueryDocumentSnapshot document : documents) {
        CaseEvent caseEvent = document.toObject(CaseEvent.class);
        long end = System.currentTimeMillis();
        System.out.printf("Read %5d %dms %s\n", uprnNumber, (end-start), caseEvent.getEvent().getTransactionId());
      }
    }
  }

  void run() throws Exception {
    //createCasesAsynchronous(100);
    //createCasesSynchronous(100);
  
    randomRead(100000, 250);
  }

  
  private CaseEvent createCase(int uprn) { 
    CaseEvent caseEvent = new CaseEvent();
  
    Header header = new Header();
    header.setChannel("ChannelA");
    header.setDateTime(new Date());
    header.setSource("Source1");
    header.setTransactionId(UUID.randomUUID().toString());
    header.setType("Type1");   
    caseEvent.setEvent(header);
    
    Address address = new Address();
    address.setAddressLine1("3 Madison Square");
    address.setAddressLine2("Whitby");
    address.setUprn(Integer.toString(uprn));
    
    CollectionCase cc = new CollectionCase();
    cc.setAddress(address);
    
    CasePayload payload = new CasePayload();
    payload.setCollectionCase(cc);
    
    caseEvent.setPayload(payload);
    
    return caseEvent;
  }
  

  public static void main(String[] args) throws Exception {
    new FirestorePerformanceTester().run();
  }
}