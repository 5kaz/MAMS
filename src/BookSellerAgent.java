package jadelab2;

import com.sun.source.tree.BreakTree;
import jade.content.onto.basic.FalseProposition;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.*;

public class BookSellerAgent extends Agent {
    //private Hashtable catalogue;
    private Hashtable<String,Integer> catalogue;
    private BookSellerGui myGui;
    private Hashtable<String, Long> nonAvailable;
    private Integer index;

    protected void setup() {
        catalogue = new Hashtable<String,Integer>();
        nonAvailable = new Hashtable<String,Long>();
        nonAvailable.put("zzz",Long.MAX_VALUE);
        index=0;
        myGui = new BookSellerGui(this);
        myGui.display();
        //book selling service registration at DF
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("book-selling");
        sd.setName("JADE-book-trading");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        addBehaviour(new OfferRequestsServer());
        addBehaviour(new PurchaseOrdersServer());
        addBehaviour(new UpdateAvailability());
    }

    protected void takeDown() {
        //book selling service deregistration at DF
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        myGui.dispose();
        System.out.println("Seller agent " + getAID().getName() + " terminated.");
    }

    //invoked from GUI, when a new book is added to the catalogue
    public void updateCatalogue(final String title, final int price) {
        addBehaviour(new OneShotBehaviour() {
            public void action() {
                catalogue.put(title, price);
                System.out.println(getAID().getLocalName() + ": " + title + " put into the catalogue. Price = " + price);
            }
        });
    }

    public Integer getTitle(String title){
        if (nonAvailable==null)
        {
            return catalogue.get(title);
        }
        if (nonAvailable.get(title)==null){

            return catalogue.get(title);
        }
        System.out.println("A proposal has already been sent for this book! I can't offer it again.");
        return null;
    }

    private class OfferRequestsServer extends CyclicBehaviour {
        public void action() {
            //proposals only template
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();
                Integer price = getTitle(title);
                if (price != null) {
                    //title found in the catalogue, respond with its price as a proposal
                    nonAvailable.put(title,System.currentTimeMillis());
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(String.valueOf(price.intValue()));
                } else {
                    //title not found in the catalogue
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("not-available");
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }


    private class PurchaseOrdersServer extends CyclicBehaviour {
        public void action() {
            //purchase order as proposal acceptance only template
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();
                Integer price = (Integer) catalogue.remove(title);
                nonAvailable.remove(title);
                if (price != null) {
                    reply.setPerformative(ACLMessage.INFORM);
                    System.out.println(getAID().getLocalName() + ": " + title + " sold to " + msg.getSender().getLocalName());
                } else {
                    //title not found in the catalogue, sold to another agent in the meantime (after proposal submission)
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("not-available");
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    private class UpdateAvailability extends CyclicBehaviour {
        public void action() {
            List<String> list = new ArrayList<String>();
            if (nonAvailable!=null) {
                nonAvailable.forEach((key, value) -> {
                    if (System.currentTimeMillis() - value >= 1000) {
                        System.out.println("removing " + key + " from nonAvailable hashtable with value of :" + value);
                        list.add(key);
                    }
                });
            }
            if (!list.isEmpty()){
                list.forEach((strin)->{
                    nonAvailable.remove(strin);
                        });
            }
            list.clear();
            block(1000);
        }
    }
}
