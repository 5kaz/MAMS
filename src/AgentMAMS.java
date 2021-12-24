package mams;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import mams.Slot;

public class AgentMAMS extends Agent {
    private AgentGui myGui;
    private String targetBookTitle;
    private Integer money = 100;
    private Double[][] CAL;
    private ArrayList<Slot> availableSlots;
    
    //list of found sellers
    private AID[] agents;

    protected void setup() {
        targetBookTitle = "";
        
        //time interval for buyer for sending subsequent CFP
        //as a CLI argument
        int interval = 20000;
        Object[] args = getArguments();
        
        //service registration at DF
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("meeting-scheduling");
        sd.setName("JADE-meeting-scheduling");
        dfd.addServices(sd);
        try {
        DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
        fe.printStackTrace();
        }

        //Initialisation of the calendar
        Double pref=0.0;
        this.CAL = new Double[7][];//'2D array'
        for (int i = 0; i< CAL.length; i++)
            CAL[i] = new Double[24];
        for(int i = 0; i< CAL.length; i++)
            for (int y = 0; y< CAL[i].length; y++) {
                pref = (double)(int) (Math.random() * 2);
                CAL[i][y]=(pref==0?pref:Math.random());
                //for dicplay control
                String day="";
                switch (i){
                    case 0 :
                        day = ("Monday");
                        break;
                    case 1 :
                        day = ("Tuesday");
                        break;
                    case 2 :
                        day = ("Wednesday");
                        break;
                    case 3 :
                        day = ("Thursday");
                        break;
                    case 4 :
                        day = ("Friday");
                        break;
                    case 5 :
                        day = ("Saturday");
                        break;
                    case 6 :
                        day = ("Sunday");
                        break;
                }
                //System.out.println(day+" at "+y+" h my preference is " + CAL[i][y]);
            }

            myGui = new AgentGui(this, this.CAL);
            myGui.display();
    }

    //invoked from GUI, when meeting button is pressed
    public void lookForMeeting() {
        addBehaviour(new OneShotBehaviour() {
            public void action() {
                System.out.println(getAID().getLocalName() + ": Launched procedure for meeting scheduling");
                //Setup available slots list
                availableSlots = new ArrayList<Slot>();
                for(int i = 0; i< CAL.length; i++){
                    for (int y = 0; y< CAL[i].length; y++) {
                        if (CAL[i][y]!=0){
                            Slot slot = new Slot(Day.MONDAY,y,CAL[i][y]);
                            switch (i){
                                case 0:
                                    slot.setDay(Day.MONDAY);
                                    break;
                                case 1:
                                    slot.setDay(Day.TUESDAY);
                                    break;
                                case 2:
                                    slot.setDay(Day.WEDNESDAY);
                                    break;
                                case 3:
                                    slot.setDay(Day.THURSDAY);
                                    break;
                                case 4:
                                    slot.setDay(Day.FRIDAY);
                                    break;
                                case 5:
                                    slot.setDay(Day.SATURDAY);
                                    break;
                                case 6:
                                    slot.setDay(Day.SUNDAY);
                                    break;
                            }
                            availableSlots.add(slot);
                        }
                    }
                }
                System.out.println(getAID().getLocalName() + ": My available slots are : \n"+availableSlots);
                
                //Update list of available agents
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("meeting-scheduling");
                template.addServices(sd);
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    System.out.println(getAID().getLocalName() + ": the following agents have been found");
                    agents = new AID[result.length];
                    for (int i = 0; i < result.length; ++i) {
                        agents[i] = result[i].getName();
                        System.out.println(agents[i].getLocalName());
                    }
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }
                //myAgent.addBehaviour(new RequestPerformer());
            }
        });
    }

    protected void takeDown() {
        myGui.dispose();
        System.out.println("Agent " + getAID().getLocalName() + " terminated.");
    }


    private class RequestPerformer extends Behaviour {
        private AID bestSeller;
        private int bestPrice;
        private int bestID;
        private int repliesCnt = 0;
        private MessageTemplate mt;
        private int step = 0;
        private boolean tooexpensive = false;
        private long timeout = System.currentTimeMillis() + 3000;

        public void action() {
            switch (step) {
                case 0:
                    //call for proposal (CFP) to found sellers
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < agents.length; ++i) {
                        cfp.addReceiver(agents[i]);
                    }
                    cfp.setContent(targetBookTitle);
                    cfp.setConversationId("book-trade");
                    cfp.setReplyWith("cfp" + System.currentTimeMillis()); //unique value
                    myAgent.send(cfp);
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    break;
                case 1:
                    //collect proposals
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            //proposal received
                            int price = Integer.parseInt(reply.getContent());
                            if (bestSeller == null || price < bestPrice) {
                                //the best proposal as for now
                                bestPrice = price;
                                bestSeller = reply.getSender();
                            }
                        }
                        repliesCnt++;
                        if (repliesCnt >= agents.length) {
                            //all proposals have been received
                            step = 2;
                        }
                    } else {
                        block(3000);
                        if((timeout - System.currentTimeMillis())<=0&&step==1){
                            step=2;
                        }
                    }
                    break;
                case 2:
                    //best proposal consumption - purchase
                    if(bestPrice<money) {
                        ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                        order.addReceiver(bestSeller);
                        order.setContent(targetBookTitle);
                        order.setConversationId("book-trade");
                        order.setReplyWith("order" + System.currentTimeMillis());
                        myAgent.send(order);
                        mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
                                MessageTemplate.MatchInReplyTo(order.getReplyWith()));
                    }
                    else {
                        targetBookTitle = "";
                        tooexpensive=true;
                    }
                    step = 3;
                    break;

                case 3:
                    //seller confirms the transaction
                    reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            //purchase succeeded
                            System.out.println(getAID().getLocalName() + ": " + targetBookTitle + " purchased for " + bestPrice + " from " + reply.getSender().getLocalName());
                            money = money - bestPrice;
                            System.out.println(getAID().getLocalName() + ": waiting for the next purchase order. I now have : "+money+"eur");
                            targetBookTitle = "";
                            //myAgent.doDelete();
                        } else {
                            System.out.println(getAID().getLocalName() + ": purchase has failed. " + targetBookTitle + " was sold in the meantime.");
                        }
                        step = 4;    //this state ends the purchase process
                    } else {
                        block();
                    }
                    break;
            }
        }

        public boolean done() {
            if (step == 2 && bestSeller == null) {
                System.out.println(getAID().getLocalName() + ": " + targetBookTitle + " is not on sale.");
            }
            if (step == 3 && tooexpensive){
                System.out.println(getAID().getLocalName() + ": I don't have enough money to buy" + targetBookTitle + " it costs : " + bestPrice + "eur and I only have " + money + "eur !");
                System.out.println(getAID().getLocalName() + ": waiting for the next purchase order.");
            }
            //process terminates here if purchase has failed (title not on sale) or book was successfully bought
            return ((step == 2 && bestSeller == null) || (step == 3 && tooexpensive) || step == 4);
        }
    }
    
}
