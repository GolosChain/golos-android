package io.golos.golos;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Test;

import java.util.HashSet;

import eu.bittrade.libs.steemj.Golos4J;
import eu.bittrade.libs.steemj.base.models.AccountName;
import eu.bittrade.libs.steemj.enums.PrivateKeyType;

import static org.junit.Assert.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void testAddAcc() {
        String privateActiveWif =null;
        String privatePostingWif = "5JeKh4taphREBdqfKzfapu6ar3gCNPPKgG5QbUzEwmuasSAQFs3";

        if (privateActiveWif != null || privatePostingWif != null) {
            HashSet<ImmutablePair<PrivateKeyType, String>> keys = new HashSet<>();
            if (privateActiveWif != null)
                keys.add(new ImmutablePair(PrivateKeyType.ACTIVE, privateActiveWif));
            if (privatePostingWif != null)
                keys.add(new ImmutablePair(PrivateKeyType.POSTING, privatePostingWif));
            Golos4J.getInstance().addAccount(new AccountName("yuri-vlad-second"), keys, true);
        }
    }
}