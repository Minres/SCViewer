package com.minres.scviewer.database;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class EventList  implements IEventList {

	ArrayList<EventEntry> store = new ArrayList<>();
	int start = 0;
	int end = store.size();
	boolean unmodifiable = false;
			
	public class Iterator implements java.util.Iterator<EventEntry> {
		
		EventList list;
		
		private int pos;
		
		public Iterator(EventList list) {
			this.list=list;
			this.pos=-1;
		}

		@Override
		public boolean hasNext() {
			return pos<(list.end-1);
		}

		@Override
		public EventEntry next() {
			if(hasNext()) {
				pos++;
				return list.store.get(pos);
			} else
				return null;
		}

		@Override
		public void remove() {
			list.store.remove(pos);
		}
		
	}
	
	public EventList() {
	}

	private EventList(ArrayList<EventEntry> store , int start, int end) {
		this.store=store;
		this.start=start;
		this.end=end;
		this.unmodifiable=true;
	}

	@Override
	public IEventList subMap(long from, boolean includeFrom, long to) {
		//the index of the first element greater than the key, or list.size()
		int beginIndex = Collections.binarySearch(store, new EventEntry(from));
		int startIndex = beginIndex < 0? -(beginIndex + 1): beginIndex;
		int endIndex = Collections.binarySearch(store, new EventEntry(to));
		endIndex = endIndex < 0? -(endIndex + 1): endIndex+1;
		if(beginIndex>0 && !includeFrom)
			return new EventList(store, startIndex+1, endIndex);
		else
			return new EventList(store, startIndex, endIndex);
	}
	
	@Override
	public int size() {
		return end-start;
	}

	@Override
	public boolean containsKey(long key) {
		int index = Collections.binarySearch(store, new EventEntry(key));
		return index>=0 && index>=start && index<end;
	}

	@Override
	public IEvent[] get(long key) {
		int index = Collections.binarySearch(store, new EventEntry(key));
		if(index<0) return null;
		return index>=start && index<end? store.get(index).events: null;
	}

	@Override
	public IEvent[] put(long key, IEvent[] value) {
		if(unmodifiable) throw new UnsupportedOperationException();
		EventEntry e = new EventEntry(key, value);
		if(store.size()==0 || store.get(store.size()-1).timestamp < key) {
			store.add(e);
		} else {
			int index = Collections.binarySearch(store, new EventEntry(key));
	        // < 0 if element is not in the list, see Collections.binarySearch
	        if (index < 0) {
	        	index = -(index + 1);
		        store.add(index, e);
	        } else { 
	            // Insertion index is index of existing element, to add new element behind it increase index
	        	store.set(index, e);
	        }
		}
		end=store.size();
        return e.events;
	}

	@Override
	public Collection<EventEntry> entrySet() {
		if(start != 0 || end != store.size())
			return Collections.unmodifiableList(store.subList(start, end));
		else
			return Collections.unmodifiableList(store);
	}

	@Override
	public boolean isEmpty() {
		return start==end || store.isEmpty();
	}

	@Override
	public long firstKey() {
		return store.get(start).timestamp;
	}

	@Override
	public long lastKey() {
		return store.get(end-1).timestamp;
	}

	// Navigable map functions
	@Override
	public EventEntry firstEntry() {
		return  store.get(start);
	}

	@Override
	public EventEntry lastEntry() {
		return store.get(end-1);
	}

	@Override
	public EventEntry floorEntry(long key) {
		int index = Collections.binarySearch(store, new EventEntry(key));
    	if(index==-1) return null;
        // < 0 if element is not in the list, see Collections.binarySearch
        if (index < 0) {
        	index = -(index + 2);
        }
        if(index>=end)
        	return store.get(end-1);
		return index<start? null: store.get(index);
	}

	@Override
	public EventEntry ceilingEntry(long key) {
		int index = Collections.binarySearch(store, new EventEntry(key));
        // < 0 if element is not in the list, see Collections.binarySearch
        if (index < 0) 
        	index = -(index + 1);
        if(index<start)
        	return store.get(start);
		return index>=end? null: store.get(index);
	}

	@Override
	public EventEntry lowerEntry(long key) {
		int index = Collections.binarySearch(store, new EventEntry(key));
        // < 0 if element is not in the list, see Collections.binarySearch
        if (index < 0) 
        	index = -(index + 1);
    	index--;
        if(index>=end)
        	return store.get(end-1);
		return index>=end || index<start ?null:store.get(index);
	}

	@Override
	public EventEntry higherEntry(long key) {
		int index = Collections.binarySearch(store, new EventEntry(key));
        // < 0 if element is not in the list, see Collections.binarySearch
        if (index < 0) 
        	index = -(index + 1);
        else
        	index++;
        if(index<start)
        	return store.get(start);
		return index>=end || index<start ?null:store.get(index);
	}

	@Override
	public java.util.Iterator<EventEntry> iterator() {
		return new Iterator(this);
	}

}
