package mil.navy.nrl.cmf.sousa.idol.service.directory;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;

public class MapTransactions 
{
	private final LinkedList _transactions = new LinkedList();


	public interface MapTransaction 
	{
		public Serializable getKey();
		public Serializable getValue();
	}

	private static abstract class AbstractMapTransaction 
		implements MapTransaction 
	{
		private final Serializable _key;
		private final Serializable _value;
		private final Number _time;

		protected AbstractMapTransaction(Serializable key, Serializable value, Number time) 
		{
			_key = key;
			_value = value;
			_time = time;
		}

		public Serializable getKey() 
		{
			return _key;
		}

		public Serializable getValue() 
		{
			return _value;
		}

		public Number getTime()
		{
			return _time;
		}
	}

	public static class Add extends AbstractMapTransaction
	{
		public Add(Serializable key, Serializable value, Number time) 
		{
			super(key, value, time);
		}
	}

	public static class Remove extends AbstractMapTransaction
	{
		public Remove(Serializable key, Number time) 
		{
			super(key, null, time);
		}
	}

	public MapTransactions() 
	{
	}

	public void add(Serializable key, Serializable value, Number time) 
	{
		_transactions.add(new Add(key, value, time));
	}

	public void remove(Serializable key, Number time) 
	{
		_transactions.add(new Remove(key, time));
	}

	public Iterator iteratorAll() 
	{
		return _transactions.iterator();
	}

	public void clear() 
	{
		_transactions.clear();
	}

	public int size() 
	{
		return _transactions.size();
	}
}
