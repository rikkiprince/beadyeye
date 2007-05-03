package se.sics.tac.aw;
import java.io.*;

public class Client
{
	private int preferredStartDay;
	private int preferredEndDay;

	private int currentStartDay;
	private int currentEndDay;

	private int flightInDay;			// must be -1 or less if do not have an allocation
	private int flightOutDay;			// must be -1 or less if do not have an allocation

	private float flightInCost;			// must be -1 or less if do not have a flight
	private float flightOutCost;		// must be -1 or less if do not have a flight

	private float[] hotelCost = new float[Constants.HOTEL_OPEN];			// must be -1 or less if do not have an allocation
	private boolean[] goodHotel = new boolean[Constants.HOTEL_OPEN];		// false for bad hotel, true for good
	private int hotelUpgradePremium;

	private int[] entDay = new int[Constants.ENT_NUM];			// must be -1 or less if do not have an allocation
	private float[] entCost = new float[Constants.ENT_NUM];			// must be -1 or less if do not have an allocation
	private int[] entPremium = new int[Constants.ENT_NUM];

	public Client(int ps, int pe, int up, int E1, int E2, int E3)
	{
		this.deallocate();

		this.preferredStartDay = ps;
		this.preferredEndDay = pe;

		this.currentStartDay = this.preferredStartDay;
		this.currentEndDay = this.preferredEndDay;

		this.hotelUpgradePremium = up;

		this.entPremium[0] = E1;
		this.entPremium[1] = E2;
		this.entPremium[2] = E3;
	}

	public void deallocate()
	{
		// initialise hotel cost to -1
		for(int h = Constants.DAY_1; h <= Constants.PENULTIMATE_DAY; h++)
		{
			this.hotelCost[h] = -1;
			this.goodHotel[h] = false;
		}
		// initialise ent values to -1
		for(int e = 0; e < Constants.ENT_NUM; e++)
		{
			this.entCost[e] = -1;
			this.entDay[e] = -1;
		}
		// initialise start and end days to -1
		this.flightInDay = -1;
		this.flightOutDay = -1;
		this.flightInCost = -1;
		this.flightOutCost = -1;
	}

	public String scheduleGrid()
	{
		String p = "";
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);

		ps.println("=================================");
		ps.println("Day:   1  2  3  4  5 ");
		ps.println("FLIGHT");
		ps.print("Pref: ");
		for(int d=Constants.DAY_1; d<=Constants.LAST_DAY; d++)
			if(d == preferredStartDay)
				 ps.print(" A ");
			else if(d == preferredEndDay)
				ps.print(" D ");
			else ps.print("   ");
		ps.println();
		ps.print("Act:  ");
		for(int d=Constants.DAY_1; d<Constants.PENULTIMATE_DAY; d++)
			if(d == flightInDay)
				 ps.print(" A ");
			else if(d == flightOutDay)
				ps.print(" D ");
			else ps.print("   ");
		ps.println();

		ps.println("HOTEL");
		ps.print("Pref: ");
		for(int d=Constants.DAY_1; d<=Constants.LAST_DAY; d++)
			if(d >= this.preferredStartDay && d < this.preferredEndDay)
				 ps.print(" x ");
			else ps.print("   ");
		ps.println();
		ps.print("Act:  ");
		for(int d=Constants.DAY_1; d<Constants.PENULTIMATE_DAY; d++)
			if(this.hotelCost[d] >= 0)
				 ps.print(" o ");
			else ps.print("   ");
		ps.println();

		ps.println("ENT");
		ps.print("Type: ");
		for(int d=Constants.DAY_1; d<=Constants.LAST_DAY; d++)
			for(int e=0; e<Constants.ENT_NUM; e++)
				if(entDay[e] == d)
					ps.print(" "+e+" ");
		ps.println();
		ps.println("Package is " + (this.isPackageFeasible()?"feasible":"infeasible") + ".");


		return baos.toString();
	}

	public boolean isPackageFeasible()
	{
		if(	flightInDay < Constants.DAY_1 || flightInDay > Constants.PENULTIMATE_DAY ||
			flightOutDay < Constants.DAY_2 || flightOutDay > Constants.LAST_DAY || 
			(flightOutDay - flightInDay) < 1)
			return false;
		
		for(int d = flightInDay; d < flightOutDay; d++)
		{
			if(hotelCost[d] < 0)
				return false;
		}

		for(int e = 0; e < Constants.ENT_NUM; e++)
			for(int f = e+1; f < Constants.ENT_NUM; f++)
				if(withinStay(entDay[e]) && withinStay(entDay[f]) && entDay[e] == entDay[f])
					return false;

		return true;		// change to true once filled in
	}

	private boolean withinStay(int eve)
	{
		return (eve >= flightInDay && eve < flightOutDay);
	}

	private boolean goodHotel()
	{
		for(int h = flightInDay; h < flightOutDay; h++)
			if(!goodHotel[h])
				return false;

		return true;
	}

	public int utility()
	{
		if(this.isPackageFeasible())
		{
			// TRAVEL
			int travel_penalty = 100 * (Math.abs(flightInDay - preferredStartDay) + Math.abs(flightOutDay - preferredEndDay));

			// HOTEL
			int hotel_bonus = (goodHotel()?hotelUpgradePremium:0);

			// FUN
			int fun_bonus = 0;
			for(int e = 0; e < Constants.ENT_NUM; e++)
				if(withinStay(entDay[e]) && entCost[e] >=0)
					fun_bonus += entPremium[e];

			// UTILITY
			int u = 1000 - travel_penalty + hotel_bonus + fun_bonus;
			return u;
		}
		else
		{
			return 0;
		}
	}

	public float cost()
	{
		float flightIn = ((flightInCost >= 0)?flightInCost:0);
		float flightOut = ((flightOutCost >= 0)?flightOutCost:0);

		float hotel = 0;
		for(int h = Constants.DAY_1; h <= Constants.PENULTIMATE_DAY; h++)
		{
			if(this.hotelCost[h] >= 0)
				hotel += this.hotelCost[h];
		}

		float entertainment = 0;
		for(int e = 0; e < Constants.ENT_NUM; e++)
		{
			if(this.entCost[e] >= 0)
				entertainment += this.entCost[e];
		}

		return flightIn + flightOut + hotel + entertainment;
	}

	public int nights()
	{
		int s = start();
		int e = end();

		if(s<e) return e-s;
		else return 0;
	}

	public int start()
	{
		return ((flightInDay >= Constants.DAY_1)?flightInDay:currentStartDay);
	}

	public int end()
	{
		return ((flightOutDay >= Constants.DAY_1)?flightOutDay:currentEndDay);
	}

	public int hotelType()
	{
		for(int h = currentStartDay; h < currentEndDay; h++)
		{
			if(hotelCost[h] >= 0)
				if(goodHotel[h])
					return TACAgent.TYPE_GOOD_HOTEL;
				else
					return TACAgent.TYPE_CHEAP_HOTEL;
		}

		if((this.hotelUpgradePremium/nights()) > 50)	// calculate this based on stats - how much more is good hotel normally?
			return TACAgent.TYPE_GOOD_HOTEL;
		else
			return TACAgent.TYPE_CHEAP_HOTEL;
	}

	public int hotelPremium()
	{
		if(hotelType() == TACAgent.TYPE_GOOD_HOTEL) return this.hotelUpgradePremium/nights();
		
		return 0;
	}

	public boolean doYouWant(int category, int type, int day, float price)
	{
		switch(category)
		{
			case TACAgent.CAT_HOTEL:			if(day >= currentStartDay && day < currentEndDay && hotelCost[day] < 0 && type == hotelType())
												{
													hotelCost[day] = price;
													return true;
												}
												break;
			case TACAgent.CAT_FLIGHT:			
												break;
			case TACAgent.CAT_ENTERTAINMENT:	
												break;
		}
		return false;
	}

	public boolean hasFlightIn()
	{
		return !(this.flightInDay < 0 || this.flightInCost < 0);
	}

	public boolean hasFlightOut()
	{
		return !(this.flightOutDay < 0 || this.flightOutCost <= -1);
	}

	public boolean hasCompleteHotelPackage()
	{
		boolean previous = goodHotel[currentStartDay];

		for(int h = currentStartDay; h < currentEndDay; h++)
		{
			if(hotelCost[h] < 0)
				return false;
			
			if(previous == goodHotel[h])
			{
				previous = goodHotel[h];
			}
			else return false;
		}

		return true;
	}

	/*public void auctionClosed()
	{
	}*/
}
