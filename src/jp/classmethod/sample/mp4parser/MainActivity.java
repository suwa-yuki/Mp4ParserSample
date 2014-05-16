package jp.classmethod.sample.mp4parser;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.List;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.view.View.OnClickListener;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;
import com.googlecode.mp4parser.authoring.tracks.TextTrackImpl;

public class MainActivity extends FragmentActivity implements LoaderCallbacks<Boolean> {

  private final MainActivity self = this;
  private ProgressDialog mProgressDialog;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    setContentView(R.layout.activity_main);
    
    findViewById(R.id.append).setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        mProgressDialog = ProgressDialog.show(self, null, null);
        Bundle args = new Bundle();
        args.putInt("type", 0);
        getSupportLoaderManager().initLoader(0, args, self);
      }
    });
    
    findViewById(R.id.crop).setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        mProgressDialog = ProgressDialog.show(self, null, null);
        Bundle args = new Bundle();
        args.putInt("type", 1);
        getSupportLoaderManager().initLoader(0, args, self);
      }
    });
    
    findViewById(R.id.sub_title).setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        mProgressDialog = ProgressDialog.show(self, null, null);
        Bundle args = new Bundle();
        args.putInt("type", 2);
        getSupportLoaderManager().initLoader(0, args, self);
      }
    });

  }

  @Override
  public Loader<Boolean> onCreateLoader(int id, Bundle args) {
    return new EditMovieTask(self, args.getInt("type"));
  }

  @Override
  public void onLoadFinished(Loader<Boolean> loader, Boolean succeed) {
    getSupportLoaderManager().destroyLoader(loader.getId());
    mProgressDialog.dismiss();
  }

  @Override
  public void onLoaderReset(Loader<Boolean> loader) {
  }

  public static class EditMovieTask extends AsyncTaskLoader<Boolean> {
    
    private int mType;

    public EditMovieTask(Context context, int type) {
      super(context);
      mType = type;
      forceLoad();
    }

    @Override
    public Boolean loadInBackground() {

      switch (mType) {
        case 0:
          return append();
        case 1:
          return crop();
        case 2:
          return subTitle();
      }

      return false;
    }
    
    private boolean append() {
      try {
        // 複数の動画を読み込み
        String f1 = Environment.getExternalStorageDirectory() + "/sample1.mp4";
        String f2 = Environment.getExternalStorageDirectory() + "/sample2.mp4";
        Movie[] inMovies = new Movie[]{
                MovieCreator.build(f1),
                MovieCreator.build(f2)};
  
        // 1つのファイルに結合
        List<Track> videoTracks = new LinkedList<Track>();
        List<Track> audioTracks = new LinkedList<Track>();
        for (Movie m : inMovies) {
            for (Track t : m.getTracks()) {
                if (t.getHandler().equals("soun")) {
                    audioTracks.add(t);
                }
                if (t.getHandler().equals("vide")) {
                    videoTracks.add(t);
                }
            }
        }
        Movie result = new Movie();
        if (audioTracks.size() > 0) {
            result.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
        }
        if (videoTracks.size() > 0) {
            result.addTrack(new AppendTrack(videoTracks.toArray(new Track[videoTracks.size()])));
        }
  
        // 出力
        Container out = new DefaultMp4Builder().build(result);
        String outputFilePath = Environment.getExternalStorageDirectory() + "/output_append.mp4";
        FileOutputStream fos = new FileOutputStream(new File(outputFilePath));
        out.writeContainer(fos.getChannel());
        fos.close();
      } catch (Exception e) {
        return false;
      }
      
      return true;
    }
    
    private boolean crop() {
      try {
        // オリジナル動画を読み込み
        String filePath = Environment.getExternalStorageDirectory() + "/sample1.mp4";
        Movie originalMovie = MovieCreator.build(filePath);

        // 分割
        Track track = originalMovie.getTracks().get(0);
        Movie movie = new Movie();
        movie.addTrack(new AppendTrack(new CroppedTrack(track, 200, 400)));

        // 出力
        Container out = new DefaultMp4Builder().build(movie);
        String outputFilePath = Environment.getExternalStorageDirectory() + "/output_crop.mp4";
        FileOutputStream fos = new FileOutputStream(new File(outputFilePath));
        out.writeContainer(fos.getChannel());
        fos.close();
      } catch (Exception e) {
        e.printStackTrace();
        return false;
      }
      return true;
    }
    
    private boolean subTitle() {
      try {
        // オリジナル動画を読み込み
        String filePath = Environment.getExternalStorageDirectory() + "/sample1.mp4";
        Movie countVideo = MovieCreator.build(filePath);
    
        // SubTitleを追加
        TextTrackImpl subTitleEng = new TextTrackImpl();
        subTitleEng.getTrackMetaData().setLanguage("eng");
    
        subTitleEng.getSubs().add(new TextTrackImpl.Line(0, 1000, "Five"));
        subTitleEng.getSubs().add(new TextTrackImpl.Line(1000, 2000, "Four"));
        subTitleEng.getSubs().add(new TextTrackImpl.Line(2000, 3000, "Three"));
        subTitleEng.getSubs().add(new TextTrackImpl.Line(3000, 4000, "Two"));
        subTitleEng.getSubs().add(new TextTrackImpl.Line(4000, 5000, "one"));
        countVideo.addTrack(subTitleEng);
    
        // 出力
        Container container = new DefaultMp4Builder().build(countVideo);
        String outputFilePath = Environment.getExternalStorageDirectory() + "/output_subtitle.mp4";
        FileOutputStream fos = new FileOutputStream(outputFilePath);
        FileChannel channel = fos.getChannel();
        container.writeContainer(channel);
        fos.close();
      } catch (Exception e) {
        e.printStackTrace();
        return false;
      }
      return true;
    }

  }

}
