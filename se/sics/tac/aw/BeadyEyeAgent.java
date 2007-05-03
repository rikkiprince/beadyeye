package se.sics.tac.aw;
import se.sics.tac.util.ArgEnumerator;
import java.util.logging.*;

public class BeadyEyeAgent extends AgentImpl
{
	private static final Logger log = Logger.getLogger(BeadyEyeAgent.class.getName());

	private static final boolean DEBUG = false;

	private float[] prices;

	private Flights flights;
	private ScreenAndFile OUT;

	private Client[] client = new Client[Constants.NUM_CLIENTS];

	protected void init(ArgEnumerator args)
	{
		prices = new float[agent.getAuctionNo()];
	}

	public void gameStarted()
	{
		log.fine("Game " + agent.getGameID() + " started!");

		flights = new Flights(agent);
		OUT = new ScreenAndFile("games/"+agent.getGameID()+"/log_"+agent.getGameID()+".txt");

		createClients();
		//calculateAllocation();
		//sendBids();

		initialBids();
	}

	public void gameStopped()
	{
		log.fine("Game Stopped!");
		
		// print out utility and cost information for all clients
		int utility = 0, cost = 0;
		for(int c = 0; c < Constants.NUM_CLIENTS; c++)
		{
			OUT.println("Client " + (c+1) + "'s predicted utility: " + client[c].utility() + " and cost: " + client[c].cost());
			utility += client[c].utility();
			cost += client[c].cost();
		}
		OUT.println("==========================");
		OUT.println("Utility: " + utility + "  Cost: " + cost + "  Score: " + (utility - cost));

		flights.printFlightPrices();
	}

	public void quoteUpdated(Quote quote)
	{
		//log.fine("Quote Updated");

		int auction = quote.getAuction();
		int category = agent.getAuctionCategory(auction);
		int type = agent.getAuctionType(auction);
		int day = agent.getAuctionDay(auction);
		float price = quote.getAskPrice();

		switch(category)
		{
			case TACAgent.CAT_FLIGHT:	flights.quoteUpdated(new Flight(day, type, price));
										break;
			case TACAgent.CAT_HOTEL:	Bid bid = new Bid(auction);
										if(price == 0)
										{
											bid.addBidPoint(2, 10);
											bid.addBidPoint(3, 5);
											bid.addBidPoint(4, 2);
										}
										else if(price <= 10)
										{
											bid.addBidPoint(1, 20);
											bid.addBidPoint(1, 15);
											bid.addBidPoint(2, 10);
											bid.addBidPoint(2, 5);
										}
										agent.submitBid(bid);
										break;
			default:	break;
		}

		/*if (auctionCategory == TACAgent.CAT_HOTEL)
		{
			int alloc = agent.getAllocation(auction);
			if (alloc > 0 && quote.hasHQW(agent.getBid(auction)) && quote.getHQW() < alloc)
			{
				Bid bid = new Bid(auction);
				// Can not own anything in hotel auctions...
				prices[auction] = quote.getAskPrice() + 50;
				bid.addBidPoint(alloc, prices[auction]);
				if (DEBUG)
				{
					log.finest("submitting bid with alloc=" + agent.getAllocation(auction) + " own=" + agent.getOwn(auction));
				}
				agent.submitBid(bid);
			}
		}
		else if (auctionCategory == TACAgent.CAT_ENTERTAINMENT)
		{
			int alloc = agent.getAllocation(auction) - agent.getOwn(auction);
			if (alloc != 0)
			{
				Bid bid = new Bid(auction);
				if (alloc < 0)
					prices[auction] = 200f - (agent.getGameTime() * 120f) / 720000;
				else
					prices[auction] = 50f + (agent.getGameTime() * 100f) / 720000;
				bid.addBidPoint(alloc, prices[auction]);
				if (DEBUG)
				{
					log.finest("submitting bid with alloc=" + agent.getAllocation(auction) + " own=" + agent.getOwn(auction));
				}
				agent.submitBid(bid);
			}
		}*/
	}

	public void quoteUpdated(int auctionCategory)
	{
		//log.fine("All quotes for " + agent.auctionCategoryToString(auctionCategory) + " has been updated");
	}

	public void bidUpdated(Bid bid)
	{
		//log.fine("Bid Updated: id=" + bid.getID() + " auction=" + bid.getAuction() + " state=" + bid.getProcessingStateAsString());
		//log.fine("       Hash: " + bid.getBidHash());
	}

	public void bidRejected(Bid bid)
	{
		//log.warning("Bid Rejected: " + bid.getID());
		//log.warning("      Reason: " + bid.getRejectReason() + " (" + bid.getRejectReasonAsString() + ')');
		OUT.println("Bid rejection");
		for(int i=0; i<bid.getNoBidPoints(); i++)
			OUT.println(" Bid point "+i+" for " + bid.getQuantity(i) + " at " + bid.getPrice(i) + " in auction " + bid.getAuction());
	}

	public void bidError(Bid bid, int status)
	{
		log.warning("Bid Error in auction " + bid.getAuction() + ": " + status + " (" + agent.commandStatusToString(status) + ')');
	}

	public void auctionClosed(int auction)
	{
		int category = agent.getAuctionCategory(auction);

		log.fine("*** Auction " + auction + " closed!");
		OUT.println();
		OUT.println("=============================================");
		OUT.println("Auction "+auction+" closed");
		OUT.println("Day: "+agent.getAuctionDay(auction));
		OUT.println("Type: "+agent.getAuctionTypeAsString(auction));
		OUT.println("Price: "+(agent.getQuote(auction)).getAskPrice());
		OUT.println("=============================================");
		OUT.println();

		if(category == TACAgent.CAT_HOTEL)
		{
			// look to buy cheap hotels!
		}
	}

	public void transaction(Transaction t)
	{
		int auction = t.getAuction();
		int category = agent.getAuctionCategory(auction);
		int type = agent.getAuctionType(auction);
		int day = agent.getAuctionDay(auction);
		String s = agent.getAuctionTypeAsString(auction);
		int quantity = t.getQuantity();

		OUT.println("Transaction: " + quantity + " @ " + t.getPrice() + " in " + s);

		// allocate to clients
		// for loop all transaction history
		//		.doYouWant(x)

		// for simplicity, just .doYouWant(x) for each transaction
		for(int c=0; c < Constants.NUM_CLIENTS; c++)
		{
			if(client[c].doYouWant(category, type, day))
			{
				OUT.println("Allocated to Client " + (c+1));
				quantity--;
				if(quantity <= 0)
					break;
			}
		}

		// if client have full hotel roster, order flights

		// if just doing single nights, do this:
		if(category == TACAgent.CAT_HOTEL)
		{
			flights.pleaseBuy(quantity, TACAgent.TYPE_INFLIGHT, day);
			flights.pleaseBuy(quantity, TACAgent.TYPE_OUTFLIGHT, day+1);
		}
	}



	private void createClients()
	{
		OUT.println("Printing Client info!");
		for(int c = 0; c < Constants.NUM_CLIENTS; c++)
		{
			int preferredStartDay = agent.getClientPreference(c, TACAgent.ARRIVAL);
			int preferredEndDay = agent.getClientPreference(c, TACAgent.DEPARTURE);
			int hotelUpgradePremium = agent.getClientPreference(c, TACAgent.HOTEL_VALUE);
			
			int E1 = agent.getClientPreference(c, TACAgent.E1);
			int E2 = agent.getClientPreference(c, TACAgent.E2);
			int E3 = agent.getClientPreference(c, TACAgent.E3);

			this.client[c] = new Client(preferredStartDay, preferredEndDay, hotelUpgradePremium, E1, E2, E3);
			OUT.println();
			OUT.println("*** Client " + (c+1) +" ***");
			OUT.println(this.client[c].scheduleGrid());
		}
	}



	private void initialBids()
	{
		OUT.println("Sending initial bids.");
		// hotels
		// setup array
		Bid hb[][] = new Bid[2][Constants.HOTEL_OPEN];
		for(int t=0; t<2; t++)
		{
			for(int d = Constants.DAY_1; d <= Constants.PENULTIMATE_DAY; d++)
			{
				int auction = agent.getAuctionFor(TACAgent.CAT_HOTEL, t, d);
				hb[t][d] = new Bid(auction);
			}
		}
		// create bids
		for(int c = 0; c < Constants.NUM_CLIENTS; c++)
		{
			if(client[c].nights() == 1)
			{
				/*int auction = agent.getAuctionFor(TACAgent.CAT_HOTEL, client[c].hotelType(), client[c].start());
				Bid bid = new Bid(auction);*/
				hb[client[c].hotelType()][client[c].start()].addBidPoint(1, 1000 + client[c].hotelPremium());
				//agent.submitBid(bid);
				OUT.println("Bidding in auction " + hb[client[c].hotelType()][client[c].start()].getAuction() +
					" $" + (1000 + client[c].hotelPremium()) + " for " +
					((client[c].hotelType() == TACAgent.TYPE_GOOD_HOTEL)?"good":"cheap") + " hotel on Day " + client[c].start());
			}
		}
		// submit bids
		for(int t=0; t<2; t++)
		{
			for(int d = Constants.DAY_1; d <= Constants.PENULTIMATE_DAY; d++)
			{
				if(hb[t][d].getNoBidPoints() > 0)
					agent.submitBid(hb[t][d]);
			}
		}
	}





// Their own methods...

  private void sendBids() {
    for (int i = 0, n = agent.getAuctionNo(); i < n; i++) {
      int alloc = agent.getAllocation(i) - agent.getOwn(i);
      float price = -1f;
      switch (agent.getAuctionCategory(i)) {
      case TACAgent.CAT_FLIGHT:
	if (alloc > 0) {
	  price = 1000;
	}
	break;
      case TACAgent.CAT_HOTEL:
	if (alloc > 0) {
	  price = 200;
	  prices[i] = 200f;
	}
	break;
      case TACAgent.CAT_ENTERTAINMENT:
	if (alloc < 0) {
	  price = 200;
	  prices[i] = 200f;
	} else if (alloc > 0) {
	  price = 50;
	  prices[i] = 50f;
	}
	break;
      default:
	break;
      }
      if (price > 0) {
	Bid bid = new Bid(i);
	bid.addBidPoint(alloc, price);
	if (DEBUG) {
	  log.finest("submitting bid with alloc=" + agent.getAllocation(i)
		     + " own=" + agent.getOwn(i));
	}
	agent.submitBid(bid);
      }
    }
  }





  private void calculateAllocation() {
    for (int i = 0; i < 8; i++) {
      int inFlight = agent.getClientPreference(i, TACAgent.ARRIVAL);
      int outFlight = agent.getClientPreference(i, TACAgent.DEPARTURE);
      int hotel = agent.getClientPreference(i, TACAgent.HOTEL_VALUE);
      int type;

      // Get the flight preferences auction and remember that we are
      // going to buy tickets for these days. (inflight=1, outflight=0)
      int auction = agent.getAuctionFor(TACAgent.CAT_FLIGHT,
					TACAgent.TYPE_INFLIGHT, inFlight);
      agent.setAllocation(auction, agent.getAllocation(auction) + 1);
      auction = agent.getAuctionFor(TACAgent.CAT_FLIGHT,
				    TACAgent.TYPE_OUTFLIGHT, outFlight);
      agent.setAllocation(auction, agent.getAllocation(auction) + 1);

      // if the hotel value is greater than 70 we will select the
      // expensive hotel (type = 1)
      if (hotel > 70) {
	type = TACAgent.TYPE_GOOD_HOTEL;
      } else {
	type = TACAgent.TYPE_CHEAP_HOTEL;
      }
      // allocate a hotel night for each day that the agent stays
      for (int d = inFlight; d < outFlight; d++) {
	auction = agent.getAuctionFor(TACAgent.CAT_HOTEL, type, d);
	log.finer("Adding hotel for day: " + d + " on " + auction);
	agent.setAllocation(auction, agent.getAllocation(auction) + 1);
      }

      int eType = -1;
      while((eType = nextEntType(i, eType)) > 0) {
	auction = bestEntDay(inFlight, outFlight, eType);
	log.finer("Adding entertainment " + eType + " on " + auction);
	agent.setAllocation(auction, agent.getAllocation(auction) + 1);
      }
    }
  }

  private int bestEntDay(int inFlight, int outFlight, int type) {
    for (int i = inFlight; i < outFlight; i++) {
      int auction = agent.getAuctionFor(TACAgent.CAT_ENTERTAINMENT,
					type, i);
      if (agent.getAllocation(auction) < agent.getOwn(auction)) {
	return auction;
      }
    }
    // If no left, just take the first...
    return agent.getAuctionFor(TACAgent.CAT_ENTERTAINMENT,
			       type, inFlight);
  }

  private int nextEntType(int client, int lastType) {
    int e1 = agent.getClientPreference(client, TACAgent.E1);
    int e2 = agent.getClientPreference(client, TACAgent.E2);
    int e3 = agent.getClientPreference(client, TACAgent.E3);

    // At least buy what each agent wants the most!!!
    if ((e1 > e2) && (e1 > e3) && lastType == -1)
      return TACAgent.TYPE_ALLIGATOR_WRESTLING;
    if ((e2 > e1) && (e2 > e3) && lastType == -1)
      return TACAgent.TYPE_AMUSEMENT;
    if ((e3 > e1) && (e3 > e2) && lastType == -1)
      return TACAgent.TYPE_MUSEUM;
    return -1;
}

 



  // -------------------------------------------------------------------
  // Only for backward compability
  // -------------------------------------------------------------------

  public static void main (String[] args) {
    TACAgent.main(args);
  }

} // BeadyEyeAgent
