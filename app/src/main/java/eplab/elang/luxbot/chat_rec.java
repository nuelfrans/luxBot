package eplab.elang.luxbot;


import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;


class chat_rec extends RecyclerView.ViewHolder {
    final TextView leftText;
    final TextView rightText;

    chat_rec(View itemView) {
        super(itemView);

        leftText = itemView.findViewById(R.id.leftText);
        rightText = itemView.findViewById(R.id.rightText);
    }
}
