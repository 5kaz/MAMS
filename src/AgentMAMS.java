package mams;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import javax.swing.*;
import java.io.IOException;
import java.util.*;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import mams.Slot;

public class AgentMAMS extends Agent {
    private AgentGui myGui;
    private Integer money = 100;
    private Double[][] CAL;
    private ArrayList<Slot> availableSlots;
    private Slot[] bestSlots;

    
    //list of found agents
    private AID[] agents;

    protected void setup() {
        this.bestSlots = new Slot[2];

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

        //Add cyclic behaviour for handling incoming messages
        addBehaviour(new OfferRequestsServer());

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
            //System.out.println(getAID().getLocalName() + ": My available slots are : \n"+availableSlots);
            
            //Sort the ArrayList by preference
            Collections.sort(availableSlots, Collections.reverseOrder()); 
                

            myGui = new AgentGui(this, this.CAL);
            myGui.display();
    }

    //invoked from GUI, when meeting button is pressed
    public void lookForMeeting() {
        addBehaviour(new OneShotBehaviour() {
            public void action() {
                System.out.println(getAID().getLocalName() + ": Launched procedure for meeting scheduling");
                //Update list of available agents
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("meeting-scheduling");
                template.addServices(sd);
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    agents = new AID[result.length];
                    for (int i = 0; i < result.length; ++i) {
                        agents[i] = result[i].getName();
                    }
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }
                myAgent.addBehaviour(new RequestPerformer());
            }
        });
    }

    protected void takeDown() {
        myGui.dispose();
        System.out.println("Agent " + getAID().getLocalName() + " terminated.");
    }


    private class RequestPerformer extends Behaviour {
        private MessageTemplate mt;
        private int step = 0;
   	    private int repliesCnt = 0;
        private long timeout = System.currentTimeMillis() + 3000;
        private int startIndex = 0;
        private HashMap<AID,Slot[]> othersSlots;
        private int iteration = 1;

        public void action() {
            switch (step) {
                case 0:
                    repliesCnt = 0;
                    othersSlots = new HashMap<AID,Slot[]>();
                    //Setup bestSlots list
                    bestSlots[0] = availableSlots.get(startIndex);
                    bestSlots[1] = availableSlots.get(startIndex+1);
                    System.out.println(getAID().getLocalName() + ": [Iteration "+iteration+"]  My two best slots are : "+bestSlots[0]+bestSlots[1]+"\n");
                    //call for proposal (CFP) to found sellers
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < agents.length; ++i) {
                        if (getAID().equals(agents[i]) == false){
                            cfp.addReceiver(agents[i]);
                            System.out.println(getAID().getLocalName() + ": Added "+agents[i].getLocalName()+" to receivers ");
                        }
                    }
                    //we provide the bestSlots
                    try{
                        cfp.setContentObject(bestSlots);
                    }catch(IOException e){
                        e.printStackTrace();
                    }
                    cfp.setConversationId("meeting-scheduling");
                    cfp.setReplyWith("cfp" + System.currentTimeMillis()); //unique value
                    myAgent.send(cfp);
                    System.out.println(getAID().getLocalName() + ": Sent call for proposal");

                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("meeting-scheduling"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    break;
                case 1:
                    //collect proposals
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            //proposal received
                            try{
                                othersSlots.put(reply.getSender(),(Slot[])reply.getContentObject());
                            }catch(UnreadableException e){
                                e.printStackTrace();
                            }
                        }
                        repliesCnt++;
                        if (repliesCnt >= (agents.length-1)) {
                            //all proposals have been received
                            if (repliesCnt == othersSlots.size()){
                                step = 2;
                                System.out.println(getAID().getLocalName() + ": Collected all the lists of slots");
                            }
                            else{
                                startIndex += 2;
                                iteration++;
                                step = 0;
                                System.out.println(getAID().getLocalName() + ": Restart process, some agents hadn't any corresponding slot\n---------------------------\n");
                            }
                            
                        }
                    } else {
                        block(3000);
                        if((timeout - System.currentTimeMillis())<=0&&step==1){
                            step=2;
                        }
                    }
                    break;
                case 2:
                    //Process proposals
                    Slot bestSlot = findDeal(othersSlots);
                    if (bestSlot != null) { //if a deal has been found
                        System.out.println("FOUND THIS SPOT :" + bestSlot);
                        /*ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                        order.addReceiver(bestSeller);
                        order.setContent(targetBookTitle);
                        order.setConversationId("book-trade");
                        order.setReplyWith("order" + System.currentTimeMillis());
                        myAgent.send(order);
                        mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
                                MessageTemplate.MatchInReplyTo(order.getReplyWith()));
                        */
                        step = 3;

                    }
                    else {
                    //no deal found
                        System.out.println(getAID().getLocalName() + ": Let's try with 2 other slots\n---------------------------\n");
                        startIndex += 2;
                        iteration++;
                        step = 0;
                    }                    
                    break;

                case 3:
                    //seller confirms the transaction
                    reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            //purchase succeeded

                            //myAgent.doDelete();
                        } else {
                        }
                        step = 4;    //this state ends the purchase process
                    } else {
                        block();
                    }
                    break;
            }
        }

        public boolean done() { //TODO
            if (step == 2) {
            }
            if (step == 3){
            }
            //process terminates here if purchase has failed (title not on sale) or book was successfully bought
            return ((step == 3) || step == 4);
        }

        private Slot findDeal(HashMap<AID,Slot[]> othersSlots){
            //Slot bestSlot = new Slot();
            ArrayList<Slot> possibleSlots = new ArrayList<Slot>();
            Double[] pref = new Double[2];
            Double treshold = 0.6;
            //Calc possible slots 
            for (int slotIndex=0; slotIndex<bestSlots.length; slotIndex++){
                Double preference = bestSlots[slotIndex].getPreference();
                int count = 0;
                for (Map.Entry entry : othersSlots.entrySet()){
                    Slot[] s = (Slot []) entry.getValue();
                    for (int i=0; i<s.length; i++){
                        if (bestSlots[slotIndex].isFitting(s[i])){
                            preference += s[i].getPreference();
                            count++;
                        }
                    }
                    if (count == othersSlots.size()){
                        System.out.println("Added 1 slot to possible slots");
                        possibleSlots.add(bestSlots[slotIndex]);
                        pref[slotIndex] = preference/(count+1); //Dividing by the total number of agents involved
                    }
                }
                
            }
            System.out.println("Prefs : "+pref[0] + pref[1]);            
            switch (possibleSlots.size()){
                case 0:
                    return null;
                case 1:
                    if (pref[0] == null){
                        if (pref[1] >= treshold){
                            return possibleSlots.get(0); 
                        }
                    }
                    else{
                        if (pref[0] >= treshold){
                            return possibleSlots.get(0); 
                        }
                    }
                    System.out.println("Not sufficient average preference");
                    return null;
                default:
                    int bestSlotIndex = 0;                    
                    Double highestPref = 0.0;
                    for (int i=0; i<possibleSlots.size(); i++){
                        if (pref[i] >= highestPref){
                            highestPref = pref[i];
                            bestSlotIndex = i;
                        }
                    }
                    System.out.println("Calculated best preference : "+highestPref);
                    return (possibleSlots.get(bestSlotIndex));
            }  

        }
    }

    private class OfferRequestsServer extends CyclicBehaviour {
        private Slot[] initiatorBestSlots;
        private ArrayList<Slot> fittingSlots;

        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                initiatorBestSlots = new Slot[2];
                try{
                    initiatorBestSlots = (Slot[])msg.getContentObject();
                }catch(UnreadableException e){
                    e.printStackTrace();
                }
                ACLMessage reply = msg.createReply();
                fittingSlots = new ArrayList<Slot>();
                for (Slot s : availableSlots){
                    if (s.isFitting(initiatorBestSlots[0]) || s.isFitting(initiatorBestSlots[1])){
                        fittingSlots.add(s);
                    }
                }
                Slot[] slots = new Slot[fittingSlots.size()];
                Collections.sort(fittingSlots, Collections.reverseOrder());
                for (int i=0; i<fittingSlots.size(); i++){
                    System.out.println(getAID().getLocalName()+": I have this fitting slot "+ fittingSlots.get(i));
                    slots[i] = fittingSlots.get(i);
                }
                if (fittingSlots.size() > 0){
                    reply.setPerformative(ACLMessage.PROPOSE);
                    try{
                        //Sending only the two best slots 
                        System.out.println(getAID().getLocalName()+": Sending my slots to "+msg.getSender().getLocalName());
                        //TODO
                        reply.setContentObject(slots);
                    }catch(IOException e){
                        e.printStackTrace();
                    }
                }
                else {
                    //No slot found
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("not-available");
	            }
	            myAgent.send(reply);
            }
            else{
                block();
            }
        }
    }
}
