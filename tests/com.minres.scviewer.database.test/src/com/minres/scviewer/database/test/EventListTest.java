package com.minres.scviewer.database.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.stream.Collectors;

import org.junit.Test;

import com.minres.scviewer.database.EventList;
import com.minres.scviewer.database.IEventList;

public class EventListTest {

	EventList createList(int[] times) {
		EventList list = new EventList();
		for(int time: times)
			list.put(time, null);
		return list;
	}
	
	Long[] getTimestamps(IEventList list) {
		return list.entrySet().stream().map(e->e.timestamp).collect(Collectors.toList()).toArray(new Long[] {});
	}
	
	@Test
	public void testInsertion() throws Exception {
		IEventList list = createList(new int[] {5, 7, 3, 6, 2, 0});
		assertEquals(6, list.size());
		assertArrayEquals(new Long[]{0L, 2L, 3L, 5L, 6L, 7L}, getTimestamps(list));
	}
	
	@Test
	public void testSublist() throws Exception {
		IEventList list = createList(new int[] {5, 7, 3, 8, 2, 0});
		{
			IEventList subList = list.subMap(3, true, 5);
			assertEquals(2, subList.size());
			assertArrayEquals(new Long[]{3L, 5L}, getTimestamps(subList));
		} {
			IEventList subList = list.subMap(3, false, 5);
			assertEquals(1, subList.size());
			assertArrayEquals(new Long[]{5L}, getTimestamps(subList));
		} {
			IEventList subList = list.subMap(4, true, 6);
			assertEquals(1, subList.size());
			assertArrayEquals(new Long[]{5L}, getTimestamps(subList));
		} {
			IEventList subList = list.subMap(4, false, 6);
			assertEquals(1, subList.size());
			assertArrayEquals(new Long[]{5L}, getTimestamps(subList));
		} {
			IEventList subList = list.subMap(4, true, 9);
			assertEquals(3, subList.size());
			assertArrayEquals(new Long[]{5L, 7L, 8L}, getTimestamps(subList));
		} {
			IEventList subList = list.subMap(4, false, 9);
			assertEquals(3, subList.size());
			assertArrayEquals(new Long[]{5L, 7L, 8L}, getTimestamps(subList));
		}
	}

	@Test
	public void testEntries() throws Exception {
		IEventList list = createList(new int[] {2, 5, 8, 11});
		assertEquals(2, list.firstEntry().timestamp);
		assertEquals(11, list.lastEntry().timestamp);
		
		assertNull(list.floorEntry(1));
		assertEquals(2, list.floorEntry(2).timestamp);
		assertEquals(2, list.floorEntry(3).timestamp);
		assertEquals(2, list.floorEntry(4).timestamp);
		assertEquals(5, list.floorEntry(5).timestamp);
		assertEquals(5, list.floorEntry(6).timestamp);
		assertEquals(8, list.floorEntry(10).timestamp);
		assertEquals(11, list.floorEntry(11).timestamp);
		assertEquals(11, list.floorEntry(12).timestamp);
		
		assertEquals(2, list.ceilingEntry(1).timestamp);
		assertEquals(2, list.ceilingEntry(2).timestamp);
		assertEquals(5, list.ceilingEntry(3).timestamp);
		assertEquals(5, list.ceilingEntry(4).timestamp);
		assertEquals(5, list.ceilingEntry(5).timestamp);
		assertEquals(8, list.ceilingEntry(6).timestamp);
		assertEquals(11, list.ceilingEntry(10).timestamp);
		assertEquals(11, list.ceilingEntry(11).timestamp);
		assertNull(list.ceilingEntry(12));

		assertNull(list.lowerEntry(1));
		assertNull(list.lowerEntry(2));
		assertEquals(2, list.lowerEntry(3).timestamp);
		assertEquals(2, list.lowerEntry(4).timestamp);
		assertEquals(2, list.lowerEntry(5).timestamp);
		assertEquals(5, list.lowerEntry(6).timestamp);
		assertEquals(8, list.lowerEntry(10).timestamp);
		assertEquals(8, list.lowerEntry(11).timestamp);
		assertEquals(11, list.lowerEntry(12).timestamp);
		
		assertEquals(2, list.higherEntry(1).timestamp);
		assertEquals(5, list.higherEntry(2).timestamp);
		assertEquals(5, list.higherEntry(3).timestamp);
		assertEquals(5, list.higherEntry(4).timestamp);
		assertEquals(8, list.higherEntry(5).timestamp);
		assertEquals(8, list.higherEntry(6).timestamp);
		assertEquals(11, list.higherEntry(10).timestamp);
		assertNull(list.higherEntry(11));
		assertNull(list.higherEntry(12));
	}

	@Test
	public void testSubListEntries() throws Exception {
		IEventList fullList = createList(new int[] {2, 5, 8, 11});
		IEventList list = fullList.subMap(5, true, 8);

		assertEquals(5, list.firstEntry().timestamp);
		assertEquals(8, list.lastEntry().timestamp);
		
		assertNull(list.floorEntry(1));
		assertNull(list.floorEntry(2));
		assertNull(list.floorEntry(3));
		assertNull(list.floorEntry(4));
		assertEquals(5, list.floorEntry(5).timestamp);
		assertEquals(5, list.floorEntry(6).timestamp);
		assertEquals(8, list.floorEntry(10).timestamp);
		assertEquals(8, list.floorEntry(11).timestamp);
		assertEquals(8, list.floorEntry(12).timestamp);
		
		assertEquals(5, list.ceilingEntry(1).timestamp);
		assertEquals(5, list.ceilingEntry(2).timestamp);
		assertEquals(5, list.ceilingEntry(3).timestamp);
		assertEquals(5, list.ceilingEntry(4).timestamp);
		assertEquals(5, list.ceilingEntry(5).timestamp);
		assertEquals(8, list.ceilingEntry(6).timestamp);
		assertEquals(8, list.ceilingEntry(8).timestamp);
		assertNull(list.ceilingEntry(10));
		assertNull(list.ceilingEntry(11));
		assertNull(list.ceilingEntry(12));

		assertNull(list.lowerEntry(1));
		assertNull(list.lowerEntry(2));
		assertNull(list.lowerEntry(3));
		assertNull(list.lowerEntry(4));
		assertNull(list.lowerEntry(5));
		assertEquals(5, list.lowerEntry(6).timestamp);
		assertEquals(8, list.lowerEntry(10).timestamp);
		assertEquals(8, list.lowerEntry(11).timestamp);
		assertEquals(8, list.lowerEntry(12).timestamp);
		
		assertEquals(5, list.higherEntry(1).timestamp);
		assertEquals(5, list.higherEntry(2).timestamp);
		assertEquals(5, list.higherEntry(3).timestamp);
		assertEquals(5, list.higherEntry(4).timestamp);
		assertEquals(8, list.higherEntry(5).timestamp);
		assertEquals(8, list.higherEntry(6).timestamp);
		assertNull(list.higherEntry(8));
		assertNull(list.higherEntry(10));
		assertNull(list.higherEntry(11));
		assertNull(list.higherEntry(12));
	}
	
	@Test
	public void testInterface() throws Exception {
		EventList emptyList = new EventList();
		IEventList populatedList = createList(new int[] {0, 2, 3, 5, 6, 7});
		assertEquals(0, emptyList.size());
		assertEquals(6, populatedList.size());
		assertTrue(emptyList.isEmpty());
		assertFalse(populatedList.isEmpty());
		assertFalse(emptyList.containsKey(5));
		assertTrue(populatedList.containsKey(5));
		assertFalse(populatedList.containsKey(8));
		assertNull(emptyList.get(5));
		assertNotNull(populatedList.get(5));
		assertNull(populatedList.get(8));
	}

	@Test
	public void testInterfaceSublist() throws Exception {
		IEventList fullList = createList(new int[] {0, 2, 3, 5, 6, 7});
		IEventList emptyList = fullList.subMap(3, false, 4);
		IEventList populatedList = fullList.subMap(2, true, 6);
		assertEquals(0, emptyList.size());
		assertEquals(4, populatedList.size());
		assertTrue(emptyList.isEmpty());
		assertFalse(populatedList.isEmpty());
		assertFalse(emptyList.containsKey(5));
		assertTrue(populatedList.containsKey(5));
		assertFalse(populatedList.containsKey(8));
		assertNull(emptyList.get(5));
		assertNotNull(populatedList.get(5));
		assertNull(populatedList.get(7));
	}
}
