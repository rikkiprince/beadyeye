package se.sics.tac.aw;
import se.sics.tac.util.ArgEnumerator;
import java.util.logging.*;
import java.util.*;
import java.io.*;

public class Flights
{
	private Vector prices[][] = new Vector[2][Constants.LAST_DAY+1];

	private int pleaseBuy[][] = new int[2][Constants.LAST_DAY+1];

	private ScreenAndFile OUT;

	private TACAgent agent;

	public Flights(TACAgent a)
	{
		this.agent = a;

		for(int d=Constants.DAY_1; d<=Constants.LAST_DAY; d++)
		{
			for(int t=0; t<2; t++)
			{
				prices[t][d] = new Vector();
				pleaseBuy[t][d] = 0;
			}
		}

		int gameID = agent.getGameID();
		OUT = new ScreenAndFile("games/"+gameID+"/flightprices_"+gameID+".csv");
	}

	public void quoteUpdated(Flight f)
	{
		prices[f.direction()][f.day()].add(new Float(f.price()));

		// buy immediately - no clever goings on yet...
		for(int d=Constants.DAY_1; d<=Constants.LAST_DAY; d++)
		{
			for(int t=0; t<2; t++)
			{
				if(pleaseBuy[t][d] > 0)
				{
					// submit bid
					int auction = agent.getAuctionFor(TACAgent.CAT_FLIGHT, t, d);
					Quote quote = agent.getQuote(auction);
					float ask = quote.getAskPrice();

					Bid bid = new Bid(auction);
					bid.addBidPoint(pleaseBuy[t][d], ask*2);
					agent.submitBid(bid);

					pleaseBuy[t][d] = 0;
				}
			}
		}
	}

	public void printFlightPrices()
	{
		OUT.println("FLIGHT PRICES:");
		for(int d=Constants.DAY_1; d<=Constants.LAST_DAY; d++)
		{
			for(int t=0; t<2; t++)
			{
				for(int c=0; c<prices[t][d].size(); c++)
				{
					OUT.print(prices[t][d].get(c)+",");
				}
				OUT.println();
			}
		}
	}

	public boolean pleaseBuy(int type, int day)
	{
		return pleaseBuy(1, type, day);
	}

	public boolean pleaseBuy(int quantity, int type, int day)
	{
		if(type < 0 || type > 1 || day < Constants.DAY_1 || day > Constants.LAST_DAY) return false;

		this.pleaseBuy[type][day]+=quantity;

		return true;
	}
}
