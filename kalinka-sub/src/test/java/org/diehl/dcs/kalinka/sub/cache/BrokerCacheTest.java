/*
Copyright [2017] [DCS <Info-dcs@diehl.com>]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.diehl.dcs.kalinka.sub.cache;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public class BrokerCacheTest {

	@Rule
	public ExpectedException ee = ExpectedException.none();

	private static final String ZK_CHROOT_PATH = "/apachemq/connections";

	private TestingServer zkServer;
	private ZkClient zkClient;
	private BrokerCache cache;

	@Before
	public void setUp() throws Exception {

		this.zkServer = new TestingServer(2181);
		this.zkClient = new ZkClient(this.zkServer.getConnectString());
		// this must be done after Zookeeper install in "real-world-setup"
		this.zkClient.createPersistent(ZK_CHROOT_PATH, true);
		this.cache = new BrokerCache(new ZkClient(this.zkServer.getConnectString() + ZK_CHROOT_PATH), 3, 3L, 1L);
	}

	@Test
	public void testGetWithEviction() {

		this.cache.put("1", "one");
		this.cache.put("2", "two");
		this.cache.put("3", "three");

		assertThat(this.cache.get("1"), is("one"));
		assertThat(this.cache.get("2"), is("two"));
		assertThat(this.cache.get("3"), is("three"));

		this.cache.put("4", "four");

		assertThat(this.cache.get("1"), is(nullValue()));
		assertThat(this.cache.get("2"), is("two"));
		assertThat(this.cache.get("3"), is("three"));
		assertThat(this.cache.get("4"), is("four"));

		this.cache.put("2", "dos");

		assertThat(this.cache.get("2"), is("dos"));
	}

	@Test
	public void testGetDataFromZkWithDelete() throws Exception {

		this.zkClient.createPersistent(ZK_CHROOT_PATH + "/1", "one");

		assertThat(this.cache.get("1"), is("one"));

		final CountDownLatch cdl = new CountDownLatch(1);

		zkClient.subscribeDataChanges(ZK_CHROOT_PATH + "/1", new IZkDataListener() {

			@Override
			public void handleDataDeleted(final String dataPath) throws Exception {
				System.out.println("Delete...");
				Thread.sleep(100L);
				cdl.countDown();
			}

			@Override
			public void handleDataChange(final String dataPath, final Object data) throws Exception {
				throw new RuntimeException("Not expected...");

			}
		});

		this.zkClient.delete(ZK_CHROOT_PATH + "/1");

		cdl.await(5, TimeUnit.SECONDS);

		assertThat(this.cache.get("1"), is(nullValue()));
	}

	@Test
	public void testGetDataFromZkWithUp() throws Exception {

		this.zkClient.createPersistent(ZK_CHROOT_PATH + "/1", "one");

		final CountDownLatch cdl = new CountDownLatch(1);

		zkClient.subscribeDataChanges(ZK_CHROOT_PATH + "/1", new IZkDataListener() {

			@Override
			public void handleDataDeleted(final String dataPath) throws Exception {
				throw new RuntimeException("Not expected...");
			}

			@Override
			public void handleDataChange(final String dataPath, final Object data) throws Exception {
				System.out.println("Change...");
				Thread.sleep(100L);
				cdl.countDown();

			}
		});

		this.zkClient.writeData(ZK_CHROOT_PATH + "/1", "uno");

		cdl.await(5, TimeUnit.SECONDS);

		assertThat(this.cache.get("1"), is("uno"));
	}

	@After
	public void tearDown() throws Exception {

		this.zkServer.stop();
		this.cache.delAll();
	}
}