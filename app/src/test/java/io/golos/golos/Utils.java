package io.golos.golos;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import eu.bittrade.libs.steemj.base.models.Discussion;
import eu.bittrade.libs.steemj.base.models.DiscussionWithComments;
import eu.bittrade.libs.steemj.communication.CommunicationHandler;
import eu.bittrade.libs.steemj.communication.dto.ResponseWrapperDTO;
import io.golos.golos.repository.model.GolosDiscussionItem;
import io.golos.golos.screens.story.model.StoryWithComments;
import io.golos.golos.screens.story.model.StoryWrapper;
import io.golos.golos.utils.UpdatingState;

/**
 * Created by yuri on 23.11.17.
 */

public class Utils {
    public static StoryWithComments readStoryFromResourse(String fileNameWithExtension) throws Exception {
        ObjectMapper mapper = CommunicationHandler.getObjectMapper();
        File f = new File(Utils.class.getClassLoader().getResource(fileNameWithExtension).getPath());
        ResponseWrapperDTO wrapperDTO = mapper.readValue(f, ResponseWrapperDTO.class);
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, DiscussionWithComments.class);
        List<DiscussionWithComments> stories = mapper.convertValue(wrapperDTO.getResult(), type);
        return new StoryWithComments(stories.get(0));
    }

    public static File getFileFromResources(String fileNameWithExtension) throws Exception {
        return new File(Utils.class.getClassLoader().getResource(fileNameWithExtension).getPath());
    }

    public static List<StoryWithComments> readStoriesFromResourse(String fileNameWithExtension) throws Exception {

        ObjectMapper mapper = CommunicationHandler.getObjectMapper();
        File f = new File(Utils.class.getClassLoader().getResource(fileNameWithExtension).getPath());
        ResponseWrapperDTO wrapperDTO = mapper.readValue(f, ResponseWrapperDTO.class);
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, Discussion.class);
        List<Discussion> discussions = mapper.convertValue(wrapperDTO.getResult(), type);
        final List<StoryWithComments> stories = new ArrayList();
        discussions.forEach(new Consumer<Discussion>() {
            @Override
            public void accept(Discussion discussion) {
                stories.add(new StoryWithComments(new StoryWrapper(new GolosDiscussionItem(discussion, null),
                        UpdatingState.DONE, false, ""),
                        new ArrayList()
                ));
            }
        });
        return stories;
    }
}
