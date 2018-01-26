import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;

import eu.bittrade.libs.steemj.base.models.Discussion;
import eu.bittrade.libs.steemj.communication.CommunicationHandler;
import eu.bittrade.libs.steemj.communication.dto.ResponseWrapperDTO;
import io.golos.golos.Utils;
import io.golos.golos.screens.stories.model.StripeItem;
import io.golos.golos.repository.model.GolosDiscussionItem;
import io.golos.golos.screens.story.model.ImageRow;
import io.golos.golos.repository.model.ItemType;
import io.golos.golos.screens.story.model.Row;
import io.golos.golos.screens.story.model.StoryParserToRows;
import io.golos.golos.screens.story.model.StoryWithComments;
import io.golos.golos.screens.story.model.StoryWrapper;
import kotlin.text.Regex;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertTrue;

/**
 * Created by yuri on 01.11.17.
 */

public class ParseTest {
    Regex regex = new Regex("!\\\\[.*]\\\\(.*\\\\)|^(?:([^:/?#]+):)?(?://([^/?#]*))?([^?#]*\\\\.(?:jpg|gif|png))(?:\\\\?([^#]*))?(?:#(.*))?");

    @Test
    public void testParse() throws Exception {


        ObjectMapper mapper = CommunicationHandler.getObjectMapper();
        File f = new File(this.getClass().getClassLoader().getResource("stripe.json").getPath());
        ResponseWrapperDTO wrapperDTO = mapper.readValue(f, ResponseWrapperDTO.class);
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, Discussion.class);
        List<Discussion> discussions = mapper.convertValue(wrapperDTO.getResult(), type);

        List<GolosDiscussionItem> items = new ArrayList<>();

        for (Discussion d : discussions) {
            items.add(new GolosDiscussionItem(d, null));
        }
        HashMap<Long, Discussion> map = sort(discussions);
        assertTrue(items.get(0).getType() == ItemType.IMAGE_FIRST);
        assertTrue(items.get(1).getType() == ItemType.PLAIN_WITH_IMAGE);
        assertTrue(items.get(2).getType() == ItemType.PLAIN_WITH_IMAGE);
        assertTrue(items.get(3).getType() == ItemType.IMAGE_FIRST);
        assertTrue(items.get(4).getType() == ItemType.PLAIN_WITH_IMAGE);
        assertTrue(items.get(5).getType() == ItemType.PLAIN_WITH_IMAGE);
        assertTrue(items.get(6).getType() == ItemType.IMAGE_FIRST);
        assertTrue(items.get(7).getType() == ItemType.IMAGE_FIRST);
        assertTrue(items.get(8).getType() == ItemType.IMAGE_FIRST);
        assertTrue(items.get(9).getType() == ItemType.PLAIN_WITH_IMAGE);
        assertTrue(items.get(10).getType() == ItemType.IMAGE_FIRST);
        assertTrue(items.get(11).getType() == ItemType.IMAGE_FIRST);
        assertTrue(items.get(12).getType() == ItemType.PLAIN_WITH_IMAGE);
        assertTrue(items.get(13).getType() == ItemType.PLAIN_WITH_IMAGE);
        assertTrue(items.get(14).getType() == ItemType.PLAIN_WITH_IMAGE);
        assertTrue(items.get(15).getType() == ItemType.PLAIN_WITH_IMAGE);
        assertTrue(items.get(16).getType() == ItemType.PLAIN_WITH_IMAGE);
        assertTrue(items.get(17).getType() == ItemType.IMAGE_FIRST);
        assertTrue(items.get(18).getType() == ItemType.PLAIN_WITH_IMAGE);
        assertTrue(items.get(19).getType() == ItemType.IMAGE_FIRST);
        assertTrue(items.get(20).getType() == ItemType.IMAGE_FIRST);
        assertTrue(items.get(21).getType() == ItemType.IMAGE_FIRST);
        assertTrue(items.get(22).getType() == ItemType.PLAIN_WITH_IMAGE);
        assertTrue(items.get(23).getType() == ItemType.IMAGE_FIRST);
        assertTrue(items.get(24).getType() == ItemType.PLAIN_WITH_IMAGE);
        assertTrue(items.get(25).getType() == ItemType.PLAIN_WITH_IMAGE);
        assertTrue(items.get(26).getType() == ItemType.PLAIN_WITH_IMAGE);
        assertTrue(items.get(27).getType() == ItemType.IMAGE_FIRST);
        assertTrue(items.get(28).getType() == ItemType.PLAIN_WITH_IMAGE);
        assertTrue(items.get(29).getType() == ItemType.IMAGE_FIRST);
        assertTrue(items.get(30).getType() == ItemType.IMAGE_FIRST);
        assertTrue(items.get(31).getType() == ItemType.IMAGE_FIRST);
        assertTrue(items.get(32).getType() == ItemType.IMAGE_FIRST);
        assertTrue(items.get(33).getType() == ItemType.PLAIN_WITH_IMAGE);
        assertTrue(items.get(34).getType() == ItemType.IMAGE_FIRST);
        assertTrue(items.get(35).getType() == ItemType.PLAIN_WITH_IMAGE);
        assertTrue(items.get(36).getType() == ItemType.IMAGE_FIRST);
        assertTrue(items.get(37).getType() == ItemType.IMAGE_FIRST);
        assertTrue(items.get(38).getType() == ItemType.IMAGE_FIRST);
        assertTrue(items.get(39).getType() == ItemType.IMAGE_FIRST);
        StripeItem item = new StripeItem(map.get(2339567L));
        assertTrue(item.getBody().contains("Задание фотолаборатории оказалось для меня легким и в тоже время сложным для выбора фотографий"));
        item = new StripeItem(map.get(2337481L));
        assertFalse(item.getBody().contains("*"));
    }

    @Test
    public void testSecond() throws Exception {
        ObjectMapper mapper = CommunicationHandler.getObjectMapper();
        File f = new File(this.getClass().getClassLoader().getResource("stripe2.json").getPath());
        ResponseWrapperDTO wrapperDTO = mapper.readValue(f, ResponseWrapperDTO.class);
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, Discussion.class);
        List<Discussion> discussions = mapper.convertValue(wrapperDTO.getResult(), type);
        HashMap<Long, Discussion> map = sort(discussions);

        assertTrue(new StripeItem(map.get(2363255L)).getType() == ItemType.IMAGE_FIRST);
        assertTrue(new StripeItem(map.get(2363250L)).getType() == ItemType.IMAGE_FIRST);
        assertTrue(new StripeItem(map.get(2359922L)).getType() == ItemType.IMAGE_FIRST);
        StripeItem item = new StripeItem(map.get(2346156L));
        assertTrue(item.getType() == ItemType.PLAIN_WITH_IMAGE);
        assertTrue(item.getBody().length() > 0);

        assertFalse(item.getBody().contains("https://i.imgsafe.org/86/8656e3d010.jpeg"));
    }

    @Test//2415858
    public void testThird() throws Exception {
        ObjectMapper mapper = CommunicationHandler.getObjectMapper();
        File f = new File(this.getClass().getClassLoader().getResource("stripe3.json").getPath());
        ResponseWrapperDTO wrapperDTO = mapper.readValue(f, ResponseWrapperDTO.class);
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, Discussion.class);
        List<Discussion> discussions = mapper.convertValue(wrapperDTO.getResult(), type);
        HashMap<Long, Discussion> map = sort(discussions);
        List<StripeItem> items = new ArrayList<>();
        assertTrue(new StripeItem(map.get(2420647L)).getType() == ItemType.PLAIN);
    }


    @Test//2415858
    public void testTFourth() throws Exception {
        ObjectMapper mapper = CommunicationHandler.getObjectMapper();
        File f = new File(this.getClass().getClassLoader().getResource("stripe4.json").getPath());
        ResponseWrapperDTO wrapperDTO = mapper.readValue(f, ResponseWrapperDTO.class);
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, Discussion.class);
        List<Discussion> discussions = mapper.convertValue(wrapperDTO.getResult(), type);
        HashMap<Long, Discussion> map = sort(discussions);
        assertTrue(new StripeItem(map.get(2418408L)).getType() == ItemType.IMAGE_FIRST);
    }

 /*   @Test//2415858
    public void testFifth() throws Exception {
        ObjectMapper mapper = CommunicationHandler.getObjectMapper();
        File f = new File(this.getClass().getClassLoader().getResource("stripe5.json").getPath());
        ResponseWrapperDTO wrapperDTO = mapper.readValue(f, ResponseWrapperDTO.class);
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, Discussion.class);
        List<Discussion> discussions = mapper.convertValue(wrapperDTO.getResult(), type);
        HashMap<Long, Discussion> map = sort(discussions);
        List<StoryTree> stories = new ArrayList<StoryTree>();

        StoryTree story = new StoryTree(map.get(2516636L),null);
      //  assertTrue(new story.getType() == ItemType.IMAGE_FIRST);
        //2516636
        System.out.println(story);
    }
*/

    @Test
    public void copyTest() throws Exception {
        ObjectMapper mapper = CommunicationHandler.getObjectMapper();
        File f = new File(this.getClass().getClassLoader().getResource("stripe2.json").getPath());
        ResponseWrapperDTO wrapperDTO = mapper.readValue(f, ResponseWrapperDTO.class);
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, Discussion.class);
        List<Discussion> discussions = mapper.convertValue(wrapperDTO.getResult(), type);
        StripeItem original = new StripeItem(discussions.get(0));
        StripeItem copy = original.makeCopy();
        assertEquals(original, copy);
        original.setAvatarPath("https://i.imgsafe.org/86/8656e3d010.jpeg");
        assertNotSame(original.makeCopy(), copy);
    }

    private HashMap<Long, Discussion> sort(List<Discussion> discussions) {
        HashMap<Long, Discussion> discussionHashMap = new HashMap<>();
        for (Discussion d : discussions) {
            discussionHashMap.put(d.getId(), d);
        }
        return discussionHashMap;
    }


    @Test
    public void testRegexp() {
        String s = "![time_of_synergis.jpg](https://images.golos.io/DQmWkXMLudmDwCQAjMurQyYbzwwQrRVpBUPyJJyDMe7ZGXb/time_of_synergis.jpg)\\n\\n# Антихайп выводы\\n\\nМир давно и бесславно разделился: одни ненавидят блокчейн за его сложности, другие - не принимают из-за простоты. Кто-то твердит, что всё это - пыль, но им не вторят вторые, утверждающие, что за сим - будущее. Самое же странное положение у тех, кто между...\\n\\n## PayPal и другие бредовые идеи\\n\\nНужно понимать, что мир IT - сам по себе нереален. Он есть оцифровка мира обычного. Но порой эта, виртуальная, действительность создаёт то, что кажется нереальным вдвойне:\\n\\nhttps://www.youtube.com/watch?v=PGa0idnvDd4\\n\\nДень";
        System.out.println(s.split("!\\[.*]\\(.*\\)|^(?:([^:/?#]+):)?(?:([^/?#]*))?([^?#]*\\.(?:jpg|gif|png))(?:\\?([^#]*))?(?:#(.*))?").length);
    }

    @Test
    public void testTree() throws Exception {
        StoryWithComments tree = Utils.readStoryFromResourse("story.json");

        List<StoryWrapper> comts = tree.getFlataned();
        StoryWrapper cmt = comts.stream().filter(new Predicate<StoryWrapper>() {
            @Override
            public boolean test(StoryWrapper comment) {
                return comment.getStory().getId() == 2467386L;//10^2+10^2=200, 20^2=400, смысл есть. И такое новшество без введения линейности не приведет к добру
            }
        }).findFirst().get();
        int position = comts.indexOf(cmt);
        assertEquals(2467489L/*за пост награда считается как (а+в+с...)^2*/, comts.get(position + 1).getStory().getId());

        cmt = comts.stream().filter(new Predicate<StoryWrapper>() {
            @Override
            public boolean test(StoryWrapper golosDiscussionItem) {
                return golosDiscussionItem.getStory().getId() == 2459992;//нет никакой нужды собирать все слагаемые в одном акке
            }
        }).findFirst().get();
        position = comts.indexOf(cmt);
        assertEquals(2460229/*а, понял, этот вопрос пока не изучал*/, comts.get(position + 1).getStory().getId());


    }

    @Test
    public void testTreeTwo() throws Exception {
        StoryWithComments tree = Utils.readStoryFromResourse("story2.json");
        List<StoryWrapper> comts = tree.getFlataned();
        System.out.println(comts);
    }

    @Test
    public void storyParserTest() throws Exception {
        final StoryWithComments tree = Utils.readStoryFromResourse("story4.json");
        final StoryParserToRows parser = new StoryParserToRows();
        List<Row> rows = parser.parse(tree.rootStory());
        assertEquals(new ImageRow("https://i.imgsafe.org/89e23bed21.jpg"), rows.get(0));
        assertEquals(new ImageRow("https://arcange.eu/golos-images/2017-11-06-AccountsNew.png"), rows.get(2));
    }
}
