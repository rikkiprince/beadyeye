package se.sics.tac.aw;

public class Booking
{
	private int category;
	private int type;
	private int day;

	public Booking(int c, int t, int d)
	{
		this.category = c;
		this.type = t;
		this.day = d;
	}

	public int category()
	{
		return this.category;
	}

	public int type()
	{
		return this.type;
	}

	public int day()
	{
		return this.day;
	}
}
