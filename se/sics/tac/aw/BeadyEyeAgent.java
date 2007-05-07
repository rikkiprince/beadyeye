package se.sics.tac.aw;
import se.sics.tac.util.ArgEnumerator;
import java.util.logging.*;
import java.util.*;

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

		// find out owned Entertainment
		OUT.println("Entertainment!");
		for(int a=0; a<TACAgent.getAuctionNo(); a++)
		{
			int category = agent.getAuctionCategory(a);
			int day = agent.getAuctionDay(a);
			int type = agent.getAuctionType(a);

			if(category == TACAgent.CAT_ENTERTAINMENT)
			{
				int owns = agent.getOwn(a);
				OUT.println("Auction "+a+" owns "+owns);
				
				OUT.println(" Owns "+owns+" tickets to "+agent.getAuctionTypeAsString(a)+" on day "+day);
				
				for(int c=0; c<Constants.NUM_CLIENTS; c++)
				{
					if(owns <= 0)
						break;

					if(client[c].doYouWant(category, type, day, 5))
					{
						OUT.println("  Allocated to Client " + client[c].number());
						owns--;
					}
				}
				if(owns > 0)
				{
					// do something with unallocated bookings
					// put in Vector<Booking> unallocated;

					Bid bid = new Bid(a);
					bid.addBidPoint(-owns, 80);
					agent.submitBid(bid);
				}
			}
		}

		getMoreEntertainment();

		initialBids();
	}

	public void getMoreEntertainment()
	{
		for(int c=0; c<Constants.NUM_CLIENTS; c++)
		{
			if(client[c].needsMoreEntertainment())
			{
				Entertainment ent = client[c].getBestEntertainment();
				int day = ent.day();
				int type = ent.type();
				float price = ent.price();

				int auction = agent.getAuctionFor(TACAgent.CAT_ENTERTAINMENT, type, day);

				Bid bid = new Bid(auction);
				bid.addBidPoint(1, price);
				agent.submitBid(bid);
			}
		}
	}

	public void gameStopped()
	{
		log.fine("Game Stopped!");

		
		for(int c = 0; c < Constants.NUM_CLIENTS; c++)
		{
			OUT.println();
			OUT.println("*** Client " + client[c].number() +" ***");
			OUT.println(this.client[c].scheduleGrid());
		}
		
		// print out utility and cost information for all clients
		int utility = 0, cost = 0;
		for(int c = 0; c < Constants.NUM_CLIENTS; c++)
		{
			OUT.println("Client " + client[c].number() + "'s predicted utility: " + client[c].utility() + " and cost: " + client[c].cost());
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
		float bidPrice = quote.getBidPrice();

		switch(category)
		{
			case TACAgent.CAT_FLIGHT:			flights.quoteUpdated(new Flight(day, type, price));
												break;
			case TACAgent.CAT_HOTEL:			/*OUT.println("= Bidding for cheap hotels!");
												Bid bid = new Bid(auction);
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
												agent.submitBid(bid);*/
												break;
			case TACAgent.CAT_ENTERTAINMENT:	if(bidPrice > 200)
												{
													OUT.println("----- Selling over-priced entertainment! -----");
													Bid bid = new Bid(auction);
													bid.addBidPoint(-1, bidPrice+1);
													agent.submitBid(bid);
												}
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

		/*if(category == TACAgent.CAT_HOTEL)
		{
			// look to buy cheap hotels!
			OUT.println();
			OUT.println("$$$ Minutely Client Package Update $$$");
			for(int c=0; c<Constants.NUM_CLIENTS; c++)
				OUT.println(this.client[c].scheduleGrid());
			OUT.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
			OUT.println();
		}*/

		if(category == TACAgent.CAT_HOTEL)
		{
			getMoreEntertainment();
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
		float price = t.getPrice();

		OUT.println();
		OUT.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");

		OUT.println("Transaction: " + quantity + " @ " + t.getPrice() + " in " + s);

		// order so allocated to those most likely to need it
		this.resortClients(this.client);
		/*for(int c=0; c < Constants.NUM_CLIENTS; c++)
		{
			OUT.print(" "+c+" is "+client[c].number()+" "+client[c].nights()+" nights and "+
						(client[c].hasCompleteHotelPackage()?"a complete":"an incomplete")+" hotel package and ");
					if(client[c].hasFlightIn() && client[c].hasFlightOut()) OUT.println("both flights.");
					else if(client[c].hasFlightOut()) OUT.println("an out flight.");
					else if(client[c].hasFlightIn()) OUT.println("an in flight.");
					else OUT.println("no flights.");
		}*/

		// allocate to clients
		// for loop all transaction history
		//		.doYouWant(x)

		// for simplicity, just .doYouWant(x) for each transaction
		int unalloc_quantity = quantity;
		for(int c=0; c < Constants.NUM_CLIENTS; c++)
		{
			// do this if inside the client?
			if(category == TACAgent.CAT_HOTEL || (category == TACAgent.CAT_FLIGHT && client[c].hasCompleteHotelPackage()) || 
				(category == TACAgent.CAT_ENTERTAINMENT && client[c].hasFlightIn() && client[c].hasFlightOut()))
			{
				if(client[c].doYouWant(category, type, day, price))
				{
					OUT.println(" Allocated to Client " + client[c].number());
					unalloc_quantity--;
					if(unalloc_quantity <= 0)
						break;
				}
			}
		}

		if(unalloc_quantity > 0)
		{
			// do something with unallocated bookings
			// put in Vector<Booking> unallocated;
		}

		// incorporate this into client loop above?



		// if client have full hotel roster, order flights
		for(int c=0; c < Constants.NUM_CLIENTS; c++)
		{
			if(client[c].hasCompleteHotelPackage())
			{
				//OUT.println("Client " + client[c].number() + " has a complete hotel package!");
				//OUT.println(this.client[c].scheduleGrid());
				// book flights
				if(!client[c].hasFlightIn() && !client[c].awaitingFlightIn())
				{
					/OUT.println(" Buying in flight");
					flights.pleaseBuy(1, TACAgent.TYPE_INFLIGHT, client[c].start());
					client[c].hasOrderedFlight(TACAgent.TYPE_INFLIGHT);
				}
				else if(!client[c].hasFlightOut() && !client[c].awaitingFlightOut())
				{
					//OUT.println(" Buying out flight");
					flights.pleaseBuy(1, TACAgent.TYPE_OUTFLIGHT, client[c].end());
					client[c].hasOrderedFlight(TACAgent.TYPE_OUTFLIGHT);
				}
				else
				{
					//OUT.println(" Client "+client[c].number()+" - HI:"+client[c].hasFlightIn()+" AI:"+client[c].awaitingFlightIn()+" HO:"+client[c].hasFlightOut()+" AO:"+client[c].awaitingFlightOut());
				}
			}
		}

		// if just doing single nights, do this:
		/*if(category == TACAgent.CAT_HOTEL)
		{
			flights.pleaseBuy(quantity, TACAgent.TYPE_INFLIGHT, day);
			flights.pleaseBuy(quantity, TACAgent.TYPE_OUTFLIGHT, day+1);
		}*/
		
		OUT.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
		OUT.println();
	}


	private void resortClients(Client[] c)
	{
		Arrays.sort(c, Collections.reverseOrder());
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

			this.client[c] = new Client(c, preferredStartDay, preferredEndDay, hotelUpgradePremium, E1, E2, E3);
			OUT.println();
			//OUT.println("*** Client " + client[c].number() +" ***");
			OUT.println(this.client[c].scheduleGrid());
		}
	}

	public static final int UTILITY_FOR_HOTEL = 1000;

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
		double f[][] =	{
							{1.00},						// nights() == 1
							{0.50, 0.50},				// nights() == 2
							{0.50, 0.35, 0.15},			// nights() == 3
							{0.40, 0.30, 0.20, 0.10},	// nights() == 4
						};
		// create bids
		for(int c = 0; c < Constants.NUM_CLIENTS; c++)
		{
			if(client[c].nights() <= 4)
			{
				OUT.println(""+client[c].nights()+" nights");
				for(int d = client[c].start(); d < client[c].end(); d++)
				{
					int price = (int)(f[client[c].nights()-1][d - client[c].start()] * UTILITY_FOR_HOTEL) + client[c].hotelPerNightPremium();

					hb[client[c].hotelType()][d].addBidPoint(1, price);

					OUT.println(" Bidding in auction " + hb[client[c].hotelType()][d].getAuction() +
						" " + price + " for " +
						((client[c].hotelType() == TACAgent.TYPE_GOOD_HOTEL)?"good":"cheap") + " hotel on Day " + d);
				}
			}
			/*if(client[c].nights() == 1)
			{
				//int auction = agent.getAuctionFor(TACAgent.CAT_HOTEL, client[c].hotelType(), client[c].start());
				//Bid bid = new Bid(auction);
				int price = UTILITY_FOR_HOTEL + client[c].hotelPerNightPremium();
				hb[client[c].hotelType()][client[c].start()].addBidPoint(1, price);
				//agent.submitBid(bid);
				OUT.println("Bidding in auction " + hb[client[c].hotelType()][client[c].start()].getAuction() +
					" $" + price + " for " +
					((client[c].hotelType() == TACAgent.TYPE_GOOD_HOTEL)?"good":"cheap") + " hotel on Day " + client[c].start());
			}
			else if(client[c].nights() == 2)
			{
				for(int d = client[c].start(); d < client[c].end(); d++)
				{
					int price = (UTILITY_FOR_HOTEL/2) + client[c].hotelPerNightPremium();
					hb[client[c].hotelType()][d].addBidPoint(1, price);
					OUT.println("Bidding in auction " + hb[client[c].hotelType()][client[c].start()].getAuction() +
						" $" + price + " for " +
						((client[c].hotelType() == TACAgent.TYPE_GOOD_HOTEL)?"good":"cheap") + " hotel on Day " + client[c].start());
				}
			}
			else if(client[c].nights() == 3)
			{
				double fraction[] = {0.50, 0.35, 0.15};
				for(int d = client[c].start(); d < client[c].end(); d++)
				{
					int price = (int)(fraction[d - client[c].start()] * UTILITY_FOR_HOTEL) + client[c].hotelPerNightPremium();
					hb[client[c].hotelType()][d].addBidPoint(1, price);
					OUT.println("Bidding in auction " + hb[client[c].hotelType()][client[c].start()].getAuction() +
						" $" + price + " for " +
						((client[c].hotelType() == TACAgent.TYPE_GOOD_HOTEL)?"good":"cheap") + " hotel on Day " + client[c].start());
				}
			}
			else if(client[c].nights() == 4)
			{
				double fraction[] = {0.40, 0.30, 0.20, 0.10};
				for(int d = client[c].start(); d < client[c].end(); d++)
				{
					int price = (int)(fraction[d - client[c].start()] * UTILITY_FOR_HOTEL) + client[c].hotelPerNightPremium();
					hb[client[c].hotelType()][d].addBidPoint(1, price);
					OUT.println("Bidding in auction " + hb[client[c].hotelType()][d].getAuction() +
						" $" + price + " for " +
						((client[c].hotelType() == TACAgent.TYPE_GOOD_HOTEL)?"good":"cheap") + " hotel on Day " + d);
				}
			}*/
			/*else
			{
				for(int d = client[c].start(); d < client[c].end(); d++)
				{
					int price = (UTILITY_FOR_HOTEL/client[c].nights()) + client[c].hotelPerNightPremium();
					hb[client[c].hotelType()][d].addBidPoint(1, price);
					OUT.println("Bidding in auction " + hb[client[c].hotelType()][client[c].start()].getAuction() +
						" $" + price + " for " +
						((client[c].hotelType() == TACAgent.TYPE_GOOD_HOTEL)?"good":"cheap") + " hotel on Day " + client[c].start());
				}
			}*/
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

		// sell entertainment at more than 200 to see if anyone is silly enough to buy it!
		/*for(int a=TACAgent.MIN_ENTERTAINMENT; a<=TACAgent.MAX_ENTERTAINMENT; a++)
		{
			Bid bid = new Bid(a);
			bid.addBidPoint(-10, 210);
		}*/
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
