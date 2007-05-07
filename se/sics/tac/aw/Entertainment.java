package se.sics.tac.aw;

public class Entertainment
{
	private int day;
	private int type;
	private float price;

	public Entertainment(int d, int t, float p)
	{
		this.day = d;
		this.type = t;
		this.price = p;
	}

	public int day()
	{
		return this.day;
	}

	public int type()
	{
		return this.type;
	}

	public float price()
	{
		return this.price;
	}

	public float premium()
	{
		return this.price();
	}
}