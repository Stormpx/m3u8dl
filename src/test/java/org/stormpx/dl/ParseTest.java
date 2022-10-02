package org.stormpx.dl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stormpx.dl.kit.Strs;
import org.stormpx.dl.m3u8.*;
import org.stormpx.dl.m3u8.master.MasterList;
import org.stormpx.dl.m3u8.master.StreamInfo;
import org.stormpx.dl.m3u8.play.MediaList;
import org.stormpx.dl.m3u8.play.Segment;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

public class ParseTest {


    @Test
    public void t() throws IOException {
        var text = """
                #EXTM3U
                #EXT-X-VERSION:3
                #EXT-X-TARGETDURATION:16.500000
                #EXT-X-MEDIA-SEQUENCE:0
                #EXTINF:16.500000,
                6879310.ts
                #EXT-X-ENDLIST
                """;

        PlayList playList = new M3u8Parser().parse(new StringReader(text));
        Assertions.assertTrue(playList.isMediaFile());

    }

    @Test
    public void test1() throws IOException {
        String text = """
                   #EXTM3U
                   #EXT-X-TARGETDURATION:10
                   #EXT-X-VERSION:3
                   #EXTINF:9.009,
                   http://media.example.com/first.ts
                   #EXTINF:9.009,
                   http://media.example.com/second.ts
                   #EXTINF:3.003,
                   http://media.example.com/third.ts
                   #EXT-X-ENDLIST
                """;

        PlayList playList = new M3u8Parser().parse(new StringReader(text));
        Assertions.assertTrue(playList.isMediaFile());

        MediaList mediaList= (MediaList) playList;

        Assertions.assertEquals(10,mediaList.getTargetDuration());
        Assertions.assertEquals(3,mediaList.getVersion());

        List<PlayListElement> elements = mediaList.getElements();
        Assertions.assertEquals(3, elements.size());

        Assertions.assertEquals(((Segment)elements.get(0)).getDuration(),9.009);
        Assertions.assertEquals(((Segment)elements.get(1)).getDuration(),9.009);
        Assertions.assertEquals(((Segment)elements.get(2)).getDuration(),3.003);

        Assertions.assertTrue(mediaList.isEnd());

    }

    @Test
    public void test2() throws IOException {
        String text = """
                     #EXTM3U
                     #EXT-X-VERSION:3
                     #EXT-X-TARGETDURATION:8
                     #EXT-X-MEDIA-SEQUENCE:2680
                  
                     #EXTINF:7.975,
                     https://priv.example.com/fileSequence2680.ts
                     #EXTINF:7.941,
                     https://priv.example.com/fileSequence2681.ts
                     #EXTINF:7.975,
                     https://priv.example.com/fileSequence2682.ts
                """;
        int[] sequences={2680,2681,2682};
        double[] durations={7.975,7.941,7.975};


        PlayList playList = new M3u8Parser().parse(new StringReader(text));
        Assertions.assertTrue(playList.isMediaFile());

        MediaList mediaList= (MediaList) playList;

        Assertions.assertEquals(8,mediaList.getTargetDuration());
        Assertions.assertEquals(3,mediaList.getVersion());
        Assertions.assertEquals(2680,mediaList.getMediaSequence());
        Assertions.assertFalse(mediaList.isEnd());
        List<PlayListElement> elements = mediaList.getElements();
        Assertions.assertEquals(3, elements.size());

        for (int i = 0; i < elements.size(); i++) {
            PlayListElement element = elements.get(i);
            Segment segment = (Segment) element;
            Assertions.assertEquals(durations[i],segment.getDuration());
            Assertions.assertEquals(sequences[i],segment.getSequence());
        }

    }

    @Test
    public void test3() throws IOException {
        String text= """
                   #EXTM3U
                   #EXT-X-VERSION:3
                   #EXT-X-MEDIA-SEQUENCE:7794
                   #EXT-X-TARGETDURATION:15
                                
                   #EXT-X-KEY:METHOD=AES-128,URI="https://priv.example.com/key.php?r=52"
                                
                   #EXTINF:2.833,
                   http://media.example.com/fileSequence52-A.ts
                   #EXTINF:15.0,
                   http://media.example.com/fileSequence52-B.ts
                   #EXTINF:13.333,
                   http://media.example.com/fileSequence52-C.ts
                                
                   #EXT-X-KEY:METHOD=AES-128,URI="https://priv.example.com/key.php?r=53"
                                
                   #EXTINF:15.0,
                   http://media.example.com/fileSequence53-A.ts
                """;

        int[] sequences={7794,7795,7796,7797};
        double[] durations={2.833,15.0,13.333,15.0};


        PlayList playList = new M3u8Parser().parse(new StringReader(text));
        Assertions.assertTrue(playList.isMediaFile());

        MediaList mediaList= (MediaList) playList;

        Assertions.assertEquals(15,mediaList.getTargetDuration());
        Assertions.assertEquals(3,mediaList.getVersion());
        Assertions.assertEquals(7794,mediaList.getMediaSequence());
        Assertions.assertFalse(mediaList.isEnd());
        List<PlayListElement> elements = mediaList.getElements();
        Assertions.assertEquals(4, mediaList.getSegmentSize());


        int segmentIdx=0;

        for (PlayListElement element : elements) {
            if (element instanceof Segment segment) {
                Assertions.assertEquals(durations[segmentIdx], segment.getDuration());
                Assertions.assertEquals(sequences[segmentIdx], segment.getSequence());
                segmentIdx++;
            } else if (element instanceof EncryptInfo encryptInfo) {
                Assertions.assertEquals(EncryptMethod.AES_128,encryptInfo.getMethod());
                Assertions.assertTrue(!Strs.isBlank(encryptInfo.getUri()));
            }

        }

    }

    @Test
    public void test4() throws IOException {
        String text = """
                #EXTM3U
                #EXT-X-STREAM-INF:BANDWIDTH=1280000,AVERAGE-BANDWIDTH=1000000
                http://example.com/low.m3u8
                #EXT-X-STREAM-INF:BANDWIDTH=2560000,AVERAGE-BANDWIDTH=2000000
                http://example.com/mid.m3u8
                #EXT-X-STREAM-INF:BANDWIDTH=7680000,AVERAGE-BANDWIDTH=6000000
                http://example.com/hi.m3u8
                #EXT-X-STREAM-INF:BANDWIDTH=65000,CODECS="mp4a.40.5"
                http://example.com/audio-only.m3u8
                """;
        int[] bandwidth={1280000,2560000,7680000,65000};
        String[] uris={"http://example.com/low.m3u8","http://example.com/mid.m3u8","http://example.com/hi.m3u8","http://example.com/audio-only.m3u8"};

        PlayList playList = new M3u8Parser().parse(new StringReader(text));
        Assertions.assertFalse(playList.isMediaFile());

        MasterList masterList= (MasterList) playList;


        Assertions.assertEquals(4, masterList.getStreams().size());

        for (int i = 0; i < masterList.getStreams().size(); i++) {
            StreamInfo streamInfo = masterList.getStreams().get(i);
            Assertions.assertEquals(bandwidth[i],streamInfo.getBandwidth());
            Assertions.assertEquals(uris[i],streamInfo.getUri());
        }

        Assertions.assertTrue(masterList.getStreams().get(3).getCodecs().contains("mp4a.40.5"));

    }


    @Test
    public void test5() throws IOException {
        String text = """
                #EXTM3U
                #EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=900000,CODECS="mp4a.40.2,avc1.64001f",RESOLUTION=960x540
                video/7920a5df00b64c14bc30382b6196bbef-7d33de1855564bd1b66b777a7dd11efb-video-ld.m3u8
                #EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=1500000,CODECS="mp4a.40.2,avc1.64001f",RESOLUTION=1280x720
                video/7920a5df00b64c14bc30382b6196bbef-75f2433da1b718aed61ccd931e71304a-video-sd.m3u8
                #EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=3000000,CODECS="mp4a.40.2,avc1.640028",RESOLUTION=1920x1080
                video/7920a5df00b64c14bc30382b6196bbef-435d29af13347502057bd0d3dc04e5ba-video-hd.m3u8
                """;
        int[] bandwidth={900000,1500000,3000000};
        String[] uris={"video/7920a5df00b64c14bc30382b6196bbef-7d33de1855564bd1b66b777a7dd11efb-video-ld.m3u8",
                "video/7920a5df00b64c14bc30382b6196bbef-75f2433da1b718aed61ccd931e71304a-video-sd.m3u8",
                "video/7920a5df00b64c14bc30382b6196bbef-435d29af13347502057bd0d3dc04e5ba-video-hd.m3u8"};

        PlayList playList = new M3u8Parser().parse(new StringReader(text));
        Assertions.assertFalse(playList.isMediaFile());

        MasterList masterList= (MasterList) playList;


        Assertions.assertEquals(3, masterList.getStreams().size());

        for (int i = 0; i < masterList.getStreams().size(); i++) {
            StreamInfo streamInfo = masterList.getStreams().get(i);
            Assertions.assertEquals(bandwidth[i],streamInfo.getBandwidth());
            Assertions.assertEquals(uris[i],streamInfo.getUri());
            Assertions.assertTrue(streamInfo.getCodecs().contains("mp4a.40.2"));
            Assertions.assertTrue(streamInfo.getCodecs().contains("avc1.64001f")||streamInfo.getCodecs().contains("avc1.640028"));
        }

    }

}
