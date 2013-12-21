package info.justaway.fragment.profile;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.ProgressBar;

import info.justaway.JustawayApplication;
import info.justaway.R;
import info.justaway.adapter.FriendListAdapter;
import twitter4j.PagableResponseList;
import twitter4j.User;

public class FollowingListFragment extends Fragment {
    private FriendListAdapter adapter;
    private long userId;
    private long cursor = -1;
    private ListView listView;
    private ProgressBar mFooter;
    private int currentPage = 1;
    private int nextPage = 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.list, container, false);

        User user = (User) getArguments().getSerializable("user");
        userId = user.getId();

        // リストビューの設定
        listView = (ListView) v.findViewById(R.id.listView);

        // コンテキストメニューを使える様にする為の指定、但しデフォルトではロングタップで開く
        registerForContextMenu(listView);

        mFooter = (ProgressBar) v.findViewById(R.id.guruguru);
        mFooter.setVisibility(View.GONE);

        // Status(ツイート)をViewに描写するアダプター
        adapter = new FriendListAdapter(getActivity(), R.layout.row_user);
        listView.setAdapter(adapter);

        new FriendsListTask().execute(userId);

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                // 最後までスクロールされたかどうかの判定
                if (totalItemCount == firstVisibleItem + visibleItemCount) {
                    additionalReading();
                }
            }
        });
        return v;
    }

    private void additionalReading() {
        // 次のページあるのか確認
        if (currentPage != nextPage) {
            mFooter.setVisibility(View.VISIBLE);
            currentPage++;
            new FriendsListTask().execute(userId);
        }
        return;
    }

    private class FriendsListTask extends AsyncTask<Long, Void, PagableResponseList<User>> {
        @Override
        protected PagableResponseList<User> doInBackground(Long... params) {
            try {
                PagableResponseList<User> friendsList = JustawayApplication.getApplication().getTwitter().getFriendsList(params[0], cursor);
                cursor = friendsList.getNextCursor();
                return friendsList;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(PagableResponseList<User> friendsList) {
            for (User friendUser : friendsList) {
                adapter.add(friendUser);
            }
            mFooter.setVisibility(View.GONE);
            nextPage++;
        }
    }
}