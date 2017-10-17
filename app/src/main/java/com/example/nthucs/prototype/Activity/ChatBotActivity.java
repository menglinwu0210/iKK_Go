package com.example.nthucs.prototype.Activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.MotionEvent;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;


import ai.api.AIDataService;
import ai.api.AIListener;
import ai.api.AIServiceException;
import ai.api.android.AIConfiguration;
import ai.api.android.AIService;
import ai.api.model.AIError;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Result;

import com.example.nthucs.prototype.FoodList.CalorieDAO;
import com.example.nthucs.prototype.FoodList.Food;
import com.example.nthucs.prototype.FoodList.FoodCal;
import com.example.nthucs.prototype.FoodList.FoodDAO;
import com.example.nthucs.prototype.Settings.Health;
import com.example.nthucs.prototype.Settings.HealthDAO;
import com.example.nthucs.prototype.Activity.CalorieConsumptionActivity;
import com.example.nthucs.prototype.Activity.MyProfileActivity;
import com.example.nthucs.prototype.Settings.MyProfileDAO;
import com.example.nthucs.prototype.Settings.Profile;
import com.example.nthucs.prototype.SportList.Sport;
import com.example.nthucs.prototype.SportList.SportDAO;
import com.example.nthucs.prototype.Utility.DBFunctions;
import com.facebook.login.widget.ProfilePictureView;
import com.example.nthucs.prototype.Activity.MyWeightLossGoalActivity;
import com.google.gson.JsonElement;

import java.io.IOException;

import java.security.Policy;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nthucs.prototype.R;

import com.crashlytics.android.Crashlytics;

import org.json.JSONObject;

import io.fabric.sdk.android.Fabric;
import io.fabric.sdk.android.services.concurrency.AsyncTask;
import io.fabric.sdk.android.services.settings.SettingsRequest;

public class ChatBotActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener ,AIListener {
    private ImageButton btnSend;
    private ImageButton btnRecord;
    private AIService aiService;
    private RecyclerView recyclerView;
    private ArrayList messageArrayList;
    private ChatAdapter mAdapter;
    private EditText textMessage;
    private boolean initialRequest;

    private Activity activity = ChatBotActivity.this;
    private int activityIndex = 7;
    private static final int ChATBOT_ACTIVITY = 7;
    private static final int SCAN_FOOD = 2;
    private static final int TAKE_PHOTO = 3;
    private static final String FROM_CAMERA = "scan_food";
    private static final String FROM_GALLERY = "take_photo";
    private int flag = 0; //when flag = 0, language will be chinese ; flag = 1 will be english
    private long local_datetime;

    // To get user's blood pressure
    private Health curHealth;

    // data base for profile
    private HealthDAO healthDAO;

    // list of profile
    private List<Health> healthList = new ArrayList<>();

    // data base for profile
    private MyProfileDAO myProfileDAO;

    // list of profile
    private List<Profile> profileList = new ArrayList<>();

    private Profile curProfile;

    // data base for storing calorie data
    private CalorieDAO calorieDAO;

    // list of foodCal
    private List<FoodCal> foodCalList = new ArrayList<>();

    // data base for storing food list
    private FoodDAO foodDAO;

    // list of foods
    private List<Food> foods;

    // data base for storing sport list
    private SportDAO sportDAO;

    // list of sports
    private List<Sport> sports;

    private List<Food> todayFoods = new ArrayList<Food>();

    private List<Sport> todaySports = new ArrayList<Sport>();

    private float idel_absorb_cal, idel_consume_cal;

    MyWeightLossGoalActivity mwlga;

    //init Ai config
    AIConfiguration config = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("健康管家");
        Fabric.with(this, new Crashlytics()); //to use fabric
        setContentView(R.layout.activity_chat_bot_nav);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View headerView = navigationView.getHeaderView(0);
        TextView facebookUsername = (TextView) headerView.findViewById(R.id.Facebook_name);
        facebookUsername.setText("Hello, "+LoginActivity.facebookName);
        ProfilePictureView profilePictureView = (ProfilePictureView) headerView.findViewById(R.id.Facebook_profile_picture);
        profilePictureView.setProfileId(LoginActivity.facebookUserID);

        //  宣告 recyclerView
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        messageArrayList = new ArrayList<>();
        mAdapter = new ChatAdapter(messageArrayList);
        textMessage = (EditText) findViewById(R.id.message);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.scrollToPosition(messageArrayList.size()-1);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);
        this.textMessage.setText("");
        this.initialRequest = true;

        btnSend = (ImageButton) findViewById(R.id.btn_send);
        btnRecord= (ImageButton) findViewById(R.id.btn_record);

        //init Api ai listener
        //chinese ver.
        //change server on 09/03
        config = new AIConfiguration("a772958d63a149b39bf9f11cfad29889",
                AIConfiguration.SupportedLanguages.ChineseTaiwan,
                AIConfiguration.RecognitionEngine.System);
        aiService = AIService.getService(this, config);
        aiService.setListener(this);

        //Instruction (chatbot使用說明 => 讓user清楚知道目前聊天機器人有甚麼功能)
        Message ini_message = new Message();
        ini_message.setId("3");
        ini_message.setMessage("歡迎"+LoginActivity.facebookName +" 本聊天機器人目前支援功能有\n" +
                "1.詢問身高、體重、bmi\n" +
                "2.詢問飲食順序\n" +
                "3.詢問血壓/脈搏狀況\n"+
        "4.查詢目前消耗/吸收熱量\n"+
        "5.查詢今日所吃的食物");

        messageArrayList.add(ini_message);

        mAdapter.notifyDataSetChanged();

        btnRecord.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                aiService.startListening();
                btnRecord.setImageResource(R.drawable.icons8_ic_mic_red_24px);
                Toast.makeText(v.getContext(),"錄音中",Toast.LENGTH_SHORT).show();
            }
        });

        //by texting
        btnSend.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                senMessage();
            }
        });

    }

    public void onResult(final AIResponse response) {
        btnRecord.setImageResource(R.drawable.ic_mic_black_48dp);

        Result result = response.getResult();

        final String speech = result.getFulfillment().getSpeech();
        // Get parameters
        String parameterString = "";

        parameterString += aiResponses(parameterString,result);

        parameterString += speech;

        Message inputMessage = new Message();
        inputMessage.setMessage(result.getResolvedQuery());
        inputMessage.setAct(result.getAction());
        //inputMessage.setAns(parameterString);
        inputMessage.setId("1");
        messageArrayList.add(inputMessage);
        mAdapter.notifyDataSetChanged();
        this.initialRequest = false;

        //response
        Message outMes = new Message();
        outMes.setMessage(parameterString);
        outMes.setId("2");
        messageArrayList.add(outMes);

        //

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
                recyclerView.getLayoutManager().smoothScrollToPosition(recyclerView, null, mAdapter.getItemCount()-1);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void senMessage(){

        final String inputmes = textMessage.getText().toString().trim();

        if(!inputmes.isEmpty()) {
            final AIDataService aiDataService = new AIDataService(config);
            final AIRequest aiRequest = new AIRequest();
            aiRequest.setQuery(inputmes);
            textMessage.setText("");


            new AsyncTask<AIRequest, Void, AIResponse>() {
                @Override
                protected AIResponse doInBackground(AIRequest... requests) {
                    final AIRequest request = requests[0];
                    try {
                        final AIResponse response = aiDataService.request(request);
                        return response;
                    } catch (AIServiceException e) {
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(AIResponse aiResponse) {
                    if (aiResponse != null) {
                        String parameterString = "";
                        Result result = aiResponse.getResult();
                        final String speech = result.getFulfillment().getSpeech();

                        parameterString += aiResponses(parameterString, result);
                        parameterString += speech;

                        final Message inputMessage = new Message();
                        inputMessage.setMessage(result.getResolvedQuery());
                        inputMessage.setAct(result.getAction());

                        inputMessage.setId("1");
                        messageArrayList.add(inputMessage);
                        mAdapter.notifyDataSetChanged();
                        initialRequest = false;

                        Message outMes = new Message();
                        outMes.setMessage(parameterString);
                        outMes.setId("2");
                        messageArrayList.add(outMes);


                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mAdapter.notifyDataSetChanged();
                                recyclerView.getLayoutManager().smoothScrollToPosition(recyclerView, null, mAdapter.getItemCount() - 1);
                            }
                        });

                    }
                }
            }.execute(aiRequest);
        }
        else{
            Toast.makeText(this,"訊息不可為空！",Toast.LENGTH_SHORT).show();
        }
    }

    private String aiResponses(String parameterString,Result result) {
        //////to get blood presure
        // initialize data base
        healthDAO = new HealthDAO(getApplicationContext());
        // get all health data from data base
        healthList = healthDAO.getAll();
        // get the last health data in the list
        if (healthDAO.isTableEmpty() == true) {
            curHealth = new Health();
        } else {
            int cnt = 0;
            for (int i = 0; i < healthList.size(); i++) {
                if (healthList.get(i).getDiastolicBloodPressure() != 0
                        || healthList.get(i).getSystolicBloodPressure() != 0 || healthList.get(i).getPulse() != 0) {
                    curHealth = healthList.get(i);
                    cnt = 1;
                }
            }
            if (cnt == 0) curHealth = new Health();
        }

        //to get user profile
        // initialize data base
        myProfileDAO = new MyProfileDAO(getApplicationContext());
        profileList = myProfileDAO.getAll();

        // get the last profile data in the list
        if (myProfileDAO.isTableEmpty() == true) {
            curProfile = new Profile();
        } else {
            curProfile = profileList.get(profileList.size() - 1);
        }
        //for (final Map.Entry<String, JsonElement> entry : result.getParameters().entrySet()) {

            float sys_pre = curHealth.getSystolicBloodPressure();
            float dia_pre = curHealth.getDiastolicBloodPressure();
            float pro_height = curProfile.getHeight();
            float pro_weight = curProfile.getWeight();
            float hi = (pro_height / 100);
            float pro_BMI = pro_weight / (hi * hi);
            float tem = curHealth.getTemperature();
            int water_drunk = curHealth.getDrunkWater();

            String A_Food = new String("A_Food");
            String A_Food1 = new String("A_Food1");
            String A_Food2 = new String("A_Food2");
            String B_Food = new String("B_Food");
            String B_Food1 = new String("B_Food1");
            String B_Food2 = new String("B_Food2");
            String D_Food = new String("D_Food");
            String E_Food = new String("E_Food");
            String D_Food1 = new String("D_Food1");
            String D_Food2 = new String("D_Food2");
            String E_Food1 = new String ("E_Food1");
            String E_Food2 = new String ("E_Food2");
            String F_Food = new String("F_Food");
            String F_Food1 = new String("F_Food1");
            String G_Food = new String("G_Food");
            String G_Food1 = new String("G_Food1");
            String G_Food2 = new String("G_Food2");
            String I_Food = new String("I_Food");
            String I_Food1 = new String("I_Food1");
            String I_Food2 = new String("I_Food2");
            String J_Food = new String("J_Food");
            String J_Food1 = new String("J_Food1");
            String J_Food2 = new String("J_Food2");
            String K_Food = new String("K_Food");
            String K_Food1 = new String("K_Food1");
            String K_Food2 = new String("K_Food2");
            String L_Food = new String("L_Food");
            String L_Food1 = new String("L_Food1");
            String L_Food2 = new String("L_Food2");
            String O_Food = new String("O_Food");
            String O_Food1 = new String("O_Food1");
            String O_Food2 = new String("O_Food2");
            String O_Food3 = new String("O_Food3");
            String date = new String("date");

            float total_absorb_calories=0;

            float total_consume_calories=0;

        // to get user already have eaten food
        foodDAO = new FoodDAO(getApplicationContext());
        foods = foodDAO.getAll();

        // to get user already have exercised
        sportDAO = new SportDAO(getApplicationContext());
        sports = sportDAO.getAll();

        //database
        calorieDAO = new CalorieDAO(getApplicationContext());
        foodCalList = calorieDAO.getAll();

        //parameterString += foodCalList.get(foodCalList.size()-1).getChineseName();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/M/d");  //定義時間格式
        Date dt = new Date();  //取得目前時間
        String dts = sdf.format(dt);  //經由SimpleDateFormat將時間轉為字串

        // to get user's weight loss goal
        SharedPreferences sharedPreferences = getSharedPreferences("LossGoal",MODE_PRIVATE);

        //get today's total food
        for(int i=0;i<foods.size();i++){
            if(dts.equals(foods.get(i).getYYYYMD())){
                todayFoods.add(foods.get(i));
            }
        }

        for(int i=0;i<sports.size();i++){
            if(dts.contentEquals(sports.get(i).getYYYYMD())){
                todaySports.add(sports.get(i));
            }
        }

            if (flag == 1) {
                switch (result.getAction()) {
                    case "Get_pressure":
                        parameterString += "Your blood pressure is " + String.valueOf(sys_pre) +
                                "/" + String.valueOf(dia_pre) + ".";
                        if (sys_pre < 90.0 || dia_pre < 60.0)
                            parameterString += " You have low blood pressure,go to see a doctor, plz.";
                        else if (sys_pre >= 140.0 || dia_pre >= 90.0)
                            parameterString += " You have high blood pressure,go to see a doctor, plz.";
                        else parameterString += " Your blood pressure is normal, keep on hold";
                        break;
                    case "Get_systolic_blood_pressure":
                        parameterString += "Your systolic pressure is " + String.valueOf(sys_pre) + ".";
                        if (sys_pre < 90.0)
                            parameterString += " You have low blood pressure,go to see a doctor, plz.";
                        else if (sys_pre >= 140.0)
                            parameterString += " You have high blood pressure,go to see a doctor, plz.";
                        else parameterString += " Your systolic pressure is normal, keep on hold";
                        break;

                    case "Get_diastolic_blood_pressure":
                        parameterString += "Your diastolic pressure is " + String.valueOf(dia_pre) + ".";
                        if (dia_pre < 60.0)
                            parameterString += " You have low blood pressure,go to see a doctor, plz.";
                        else if (dia_pre >= 90.0)
                            parameterString += " You have high blood pressure,go to see a doctor, plz.";
                        else parameterString += " Your diastolic pressure is normal, keep on hold";
                        break;
                    case "blood_pressure_high_or_low":
                        if (dia_pre < 60.0)
                            parameterString += " You have low blood pressure,go to see a doctor, plz.";
                        else if (dia_pre >= 90.0)
                            parameterString += " You have high blood pressure,go to see a doctor, plz.";
                        else parameterString += " Your blood pressure is normal, keep on hold";
                        break;
                    case "Get_BMI":
                        parameterString += ("Your BMI is " + String.valueOf(pro_BMI) + ".");
                        if (pro_BMI >= 24) {
                            parameterString += " You are overweight. Do exercise and control diet, plz";
                        } else if (pro_BMI < 18.5) {
                            parameterString += " You are overweight. pay attention to balanced diet and do exercise";
                        } else parameterString += " Your BMI is normal. Congrats!";
                        break;
                    case "BMI_high_or_low":
                        if (pro_BMI >= 24) {
                            parameterString += " You are overweight. Do exercise and control diet, plz";
                        } else if (pro_BMI < 18.5) {
                            parameterString += " You are overweight. pay attention to balanced diet and do exercise";
                        } else parameterString += " Your BMI is normal. Congrats!";
                        break;
                    case "Get_height":
                        parameterString += ("Your height is " + String.valueOf(pro_height) + ".");
                        break;
                    case "Get_weight":
                        parameterString += ("Your weight is " + String.valueOf(pro_weight) + ".");
                        break;
                    case "choose_food_lunch_include":
                        parameterString += (todayFoods.get(1).toString());
                        break;
                    case "choose_food":  //The case of choosing order (English version)
                        int number = 0;

                        if(result.getStringParameter(J_Food).isEmpty() == false){ //priority 1
                            parameterString+=("You should eat ");
                            parameterString += (result.getStringParameter(J_Food));
                            number =1;

                            if (result.getStringParameter(J_Food1).isEmpty() == false) {
                                parameterString +=(",");
                                parameterString += (result.getStringParameter(J_Food1));
                            }
                            if (result.getStringParameter(J_Food2).isEmpty() == false) {
                                parameterString +=(",");
                                parameterString += (result.getStringParameter(J_Food2));
                            }

                            parameterString+=(" first.");
                        }
                        if(result.getStringParameter(D_Food).isEmpty() == false || result.getStringParameter(E_Food).isEmpty() == false || result.getStringParameter(G_Food).isEmpty() == false || result.getStringParameter(F_Food).isEmpty() == false) { //priority 2

                            if(number == 1)
                                parameterString += ("\nand then we recommend you eat ");
                            else {
                                //number = 1; //set flag
                                parameterString+=("You should eat ");
                            }
                            if (result.getStringParameter(D_Food).isEmpty() == false)
                                parameterString += (result.getStringParameter(D_Food));
                            if (result.getStringParameter(D_Food1).isEmpty() == false) {
                                parameterString +=(",");
                                parameterString += (result.getStringParameter(D_Food1));
                            }
                            if (result.getStringParameter(D_Food2).isEmpty() == false) {
                                parameterString +=(",");
                                parameterString += (result.getStringParameter(D_Food2));
                            }
                            if (result.getStringParameter(E_Food).isEmpty() == false) {
                                if(result.getStringParameter(D_Food).isEmpty() == false)
                                    parameterString +=(",");
                                parameterString += (result.getStringParameter(E_Food));
                            }
                            if (result.getStringParameter(E_Food1).isEmpty() == false) {
                                parameterString +=(",");
                                parameterString += (result.getStringParameter(E_Food1));
                            }
                            if (result.getStringParameter(E_Food2).isEmpty() == false) {
                                parameterString +=(",");
                                parameterString += (result.getStringParameter(E_Food2));
                            }
                            if (result.getStringParameter(G_Food).isEmpty() == false) {
                                if(result.getStringParameter(D_Food).isEmpty() == false || result.getStringParameter(E_Food).isEmpty() == false)
                                    parameterString +=(",");
                                parameterString += (result.getStringParameter(G_Food));
                            }
                            if (result.getStringParameter(G_Food1).isEmpty() == false) {
                                parameterString +=(",");
                                parameterString += (result.getStringParameter(G_Food1));
                            }
                            if (result.getStringParameter(G_Food2).isEmpty() == false) {
                                parameterString +=(",");
                                parameterString += (result.getStringParameter(G_Food2));
                            }

                            if (result.getStringParameter(F_Food).isEmpty() == false) {
                                if(result.getStringParameter(D_Food).isEmpty() == false || result.getStringParameter(E_Food).isEmpty() == false || result.getStringParameter(G_Food).isEmpty() == false)
                                    parameterString +=(",");
                                parameterString += (result.getStringParameter(F_Food));
                            }

                            if (result.getStringParameter(F_Food1).isEmpty() == false) {
                                parameterString +=(",");
                                parameterString += (result.getStringParameter(F_Food1));
                            }

                            if(number == 0) {
                                parameterString +=(" first.");
                                number = 1;
                            }
                        }
                        if(result.getStringParameter(B_Food).isEmpty() == false) { //priority 3
                            if(number == 1)
                                parameterString += ("\nand then we recommend you go eat ");
                            else {
                                //number = 1; //set flag
                                parameterString+=("You should eat ");
                            }
                            parameterString += ( result.getStringParameter(B_Food));

                            if (result.getStringParameter(B_Food1).isEmpty() == false) {
                                parameterString +=(",");
                                parameterString += (result.getStringParameter(B_Food1));
                            }
                            if (result.getStringParameter(B_Food2).isEmpty() == false) {
                                parameterString +=(",");
                                parameterString += (result.getStringParameter(B_Food2));
                            }

                            if(number == 0) {
                                parameterString +=(" first.");
                                number = 1;
                            }
                        }
                        if(result.getStringParameter(A_Food).isEmpty() == false || result.getStringParameter(O_Food).isEmpty() == false) { //priority 4
                            if(number == 1)
                                parameterString += ("\nand then we recommend you go eat ");
                            else {
                                //number = 1; //set flag
                                parameterString+=("You should eat ");
                            }
                            if(result.getStringParameter(A_Food).isEmpty() == false)
                                parameterString += (result.getStringParameter(A_Food));

                            if (result.getStringParameter(A_Food1).isEmpty() == false) {
                                parameterString +=(",");
                                parameterString += (result.getStringParameter(A_Food1));
                            }
                            if (result.getStringParameter(A_Food2).isEmpty() == false) {
                                parameterString +=(",");
                                parameterString += (result.getStringParameter(A_Food2));
                            }

                            if(result.getStringParameter(O_Food).isEmpty() == false) {
                                if(result.getStringParameter(A_Food).isEmpty() == false)
                                    parameterString += (",");
                                parameterString += (result.getStringParameter(O_Food));
                            }

                            if (result.getStringParameter(O_Food1).isEmpty() == false) {
                                parameterString +=(",");
                                parameterString += (result.getStringParameter(O_Food1));
                            }
                            if (result.getStringParameter(O_Food2).isEmpty() == false) {
                                parameterString +=(",");
                                parameterString += (result.getStringParameter(O_Food2));
                            }
                            if (result.getStringParameter(O_Food3).isEmpty() == false) {
                                parameterString +=(",");
                                parameterString += (result.getStringParameter(O_Food3));
                            }

                            if(number == 0) {
                                parameterString +=(" first.");
                                number = 1;
                            }
                        }
                        if(result.getStringParameter(I_Food).isEmpty() == false) { //priority 5
                            if(number == 1)
                                parameterString += ("\nand then we recommend you go eat ");
                            else {
                                //number = 1; //set flag
                                parameterString+=("You should eat ");
                            }
                            parameterString += ( result.getStringParameter(I_Food));

                            if (result.getStringParameter(I_Food1).isEmpty() == false) {
                                parameterString +=(",");
                                parameterString += (result.getStringParameter(I_Food1));
                            }
                            if (result.getStringParameter(I_Food2).isEmpty() == false) {
                                parameterString +=(",");
                                parameterString += (result.getStringParameter(I_Food2));
                            }

                            if(number == 0) {
                                parameterString +=(" first.");
                                number = 1;
                            }
                        }
                        if(result.getStringParameter(L_Food).isEmpty() == false || result.getStringParameter(K_Food).isEmpty() == false ) { //priority 6
                            if(number == 1) {
                                parameterString += ("\nand then we recommend you  ");
                                if(result.getStringParameter(L_Food).isEmpty() == false)
                                    parameterString +=("go drink ");
                                else
                                    parameterString +=("go eat ");
                            }
                            else {
                                //number = 1; //set flag
                                parameterString+=("You should eat ");
                            }
                            if(result.getStringParameter(L_Food).isEmpty() == false)
                                parameterString += (result.getStringParameter(L_Food));

                            if (result.getStringParameter(L_Food1).isEmpty() == false) {
                                parameterString +=(",");
                                parameterString += (result.getStringParameter(L_Food1));
                            }
                            if (result.getStringParameter(L_Food2).isEmpty() == false) {
                                parameterString +=(",");
                                parameterString += (result.getStringParameter(L_Food2));
                            }

                            if(result.getStringParameter(K_Food).isEmpty() == false) {
                                if(result.getStringParameter(L_Food).isEmpty() == false)
                                    parameterString += (",");
                                parameterString += (result.getStringParameter(K_Food));
                            }

                            if (result.getStringParameter(K_Food1).isEmpty() == false) {
                                parameterString +=(",");
                                parameterString += (result.getStringParameter(K_Food1));
                            }
                            if (result.getStringParameter(K_Food2).isEmpty() == false) {
                                parameterString +=(",");
                                parameterString += (result.getStringParameter(K_Food2));
                            }

                            if(number == 0) {
                                parameterString +=(" first.");
                                number = 1;
                            }
                        }
                        if(number == 0){ //default conversation ( !! FOOD ISN'T IN THE DATABASE)

                            parameterString += ("I'm sorry. The food you search isn't available in the database");
                        }
                        break;
                }
            } else {
                switch (result.getAction()) {
                    case "get_today_food":
                        int i;
                        int total_calorie=0;
                        parameterString += ("您今天吃了");
                        for(i=0;i<todayFoods.size();i++){
                            if(i!=0)
                                parameterString += ("、");
                            parameterString += (todayFoods.get(i).getTitle());
                            parameterString += (todayFoods.get(i).getGrams());
                            parameterString += ("g");
                            total_calorie += (todayFoods.get(i).getCalorie());
                        }
                        parameterString += ("\n您今天一共吃了"+total_calorie+"大卡");

                        break;
                    case "get_historic_food":
                        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy/M/d");  //定義時間格式
                        parameterString += ("您在"+result.getStringParameter(date)+"吃了 ");
                        String [] tokens = result.getStringParameter(date).split("-");
                        int year = Integer.parseInt(tokens[0]);
                        int month = Integer.parseInt(tokens[1]);
                        int day = Integer.parseInt(tokens[2]);
                        Calendar calender = Calendar.getInstance();
                        calender.set(year,month-1,day);
                        String dts1 = sdf1.format(calender.getTime());  //經由SimpleDateFormat將時間轉為字串
                        //get today's total food
                        for(i=0;i<foods.size();i++){
                            if(dts1.equals(foods.get(i).getYYYYMD())){
                                parameterString += (foods.get(i).getTitle());
                                if(i!=foods.size()-1) parameterString += "、";
                                parameterString += (foods.get(i).getFileName());
                            }
                        }
                        break;
                    case "get_pressure_info":
                        parameterString += "你的收縮壓/舒張壓為 " + String.valueOf(sys_pre) +
                                "/" + String.valueOf(dia_pre)+"\n";
                        if(sys_pre == 0 || dia_pre == 0)
                            parameterString += ("您尚未輸入血壓數值");
                        else if (sys_pre < 90.0 || dia_pre < 60.0)
                            parameterString += "正常收縮壓/舒張壓為90~140/60~90。您可能有低血壓，請休息幾分鐘再次測量。";
                        else if (sys_pre >= 140.0 || dia_pre >= 90.0)
                            parameterString += "正常收縮壓/舒張壓為90~140/60~90。您可能有高血壓，請休息幾分鐘再次測量。";
                        else parameterString += "您的血壓正常，請繼續保持";
                        break;
                    case "get_systolic_pressure_info":
                        parameterString += "你的收縮壓為 " + String.valueOf(sys_pre)+"\n";
                        if(sys_pre == 0)
                            parameterString += ("您尚未輸入血壓數值");
                        else if (sys_pre < 90.0)
                            parameterString += "正常收縮壓為90~140/60~90。您可能有低血壓，請休息幾分鐘再次測量。";
                        else if (sys_pre >= 140.0)
                            parameterString += "正常收縮壓為90~140。您可能有高血壓，請休息幾分鐘再次測量。";
                        else parameterString += "您的收縮壓正常，請繼續保持";
                        break;
                    case "get_diastolic_pressure_info":
                        parameterString += "你的舒張壓為 " + String.valueOf(dia_pre)+"\n";
                        if(dia_pre == 0)
                            parameterString += ("您尚未輸入血壓數值");
                        else if (dia_pre < 60.0) parameterString += "正常舒張壓為60~90。您可能有低血壓，請休息幾分鐘再次測量。";
                        else if (dia_pre >= 90.0)
                            parameterString += "正常舒張壓為90~140/60~90。您可能有高血壓，請休息幾分鐘再次測量。";
                        else parameterString += "您的舒張壓正常，請繼續保持";
                        break;
                    case "get_pressure_high_or_low":
                        if(sys_pre == 0 || dia_pre == 0)
                            parameterString += ("您尚未輸入血壓數值");
                        else if (sys_pre < 90.0 || dia_pre < 60.0) parameterString += "您有低血壓，請前往醫院了解詳情";
                        else if (sys_pre >= 140.0 || dia_pre >= 90.0)
                            parameterString += "您有高血壓，請前往醫院了解詳情";
                        else parameterString += "您的血壓正常，請繼續保持";
                        break;
                    case "get_bmi_info":
                        parameterString += ("您的BMI為" + String.valueOf(pro_BMI));
                        if (pro_BMI >= 24) {
                            parameterString += " 正常範圍是18.5~24。您過重了，請適量運動並控制飲食，並定期檢查BMI";
                        } else if (pro_BMI < 18.5) {
                            parameterString += " 正常範圍是18.5~24。您過輕了 均衡飲食有助身體健康";
                        } else parameterString += " BMI正常，請繼續保持";
                        break;
                    case "get_height_info":
                        parameterString += ("您的身高為" + String.valueOf(pro_height));
                        break;
                    case "get_weight_info":
                        parameterString += ("您的體重為" + String.valueOf(pro_weight));
                        break;
                    case "get_bmi_high_or_low":
                        if (pro_BMI >= 24) {
                            parameterString += " 您的體重過高，請多運動並控制飲食，並定期檢查BMI";
                        } else if (pro_BMI < 18.5) {
                            parameterString += " 您過的體太輕了 均衡飲食有助身體健康";
                        } else parameterString += " 您的BMI正常，請繼續保持";
                        break;
                    case "choose_food_action":  //The case of choosing order
                        int number = 0;
                        if(result.getStringParameter(J_Food).isEmpty() == false){ //priority 1
                            parameterString+=("建議您先吃");
                            parameterString += (result.getStringParameter(J_Food));
                            number =1;
                            /*int i;
                            for(i=0;i<foodCalList.size();i++){
                                parameterString += (foodCalList.get(i).getChineseName()+" ");
                                if(foodCalList.get(i).getChineseName().equals(result.getStringParameter(J_Food))){
                                    parameterString += ("它的熱量為 "+foodCalList.get(i).getCalorie());
                                }
                            }*/
                            if (result.getStringParameter(J_Food1).isEmpty() == false) {
                                parameterString +=(", ");
                                parameterString += (result.getStringParameter(J_Food1));
                            }
                            if (result.getStringParameter(J_Food2).isEmpty() == false) {
                                parameterString +=(", ");
                                parameterString += (result.getStringParameter(J_Food2));
                            }

                        }
                        if(result.getStringParameter(D_Food).isEmpty() == false || result.getStringParameter(E_Food).isEmpty() == false || result.getStringParameter(G_Food).isEmpty() == false || result.getStringParameter(F_Food).isEmpty() == false) { //priority 2

                            if(number == 1)
                                parameterString += ("\n接著再吃");
                            else {
                                number = 1; //set flag
                                parameterString+=("建議您先享用");
                            }
                            if (result.getStringParameter(D_Food).isEmpty() == false)
                                parameterString += (result.getStringParameter(D_Food));

                            if (result.getStringParameter(D_Food1).isEmpty() == false) {
                                parameterString +=(", ");
                                parameterString += (result.getStringParameter(D_Food1));
                            }
                            if (result.getStringParameter(D_Food2).isEmpty() == false) {
                                parameterString +=(", ");
                                parameterString += (result.getStringParameter(D_Food2));
                            }
                            if (result.getStringParameter(E_Food).isEmpty() == false) {
                                if(result.getStringParameter(D_Food).isEmpty() == false)
                                    parameterString +=(", ");
                                parameterString += (result.getStringParameter(E_Food));
                            }
                            if (result.getStringParameter(E_Food1).isEmpty() == false) {
                                parameterString +=(", ");
                                parameterString += (result.getStringParameter(E_Food1));
                            }
                            if (result.getStringParameter(E_Food2).isEmpty() == false) {
                                parameterString +=(", ");
                                parameterString += (result.getStringParameter(E_Food2));
                            }
                            if (result.getStringParameter(G_Food).isEmpty() == false) {

                                if(result.getStringParameter(D_Food).isEmpty() == false || result.getStringParameter(E_Food).isEmpty() == false) {
                                    if(result.getStringParameter(G_Food).contains("乳") || result.getStringParameter(G_Food).contains("奶") || result.getStringParameter(G_Food).contains("阿華田"))
                                        parameterString += (",喝");
                                    else
                                        parameterString += (",");
                                }

                                if(result.getStringParameter(D_Food).isEmpty() == false || result.getStringParameter(E_Food).isEmpty() == false)
                                    parameterString +=(", ");

                                parameterString += (result.getStringParameter(G_Food));
                            }

                            if (result.getStringParameter(G_Food1).isEmpty() == false) {
                                parameterString +=(", ");
                                parameterString += (result.getStringParameter(G_Food1));
                            }
                            if (result.getStringParameter(G_Food2).isEmpty() == false) {
                                parameterString +=(", ");
                                parameterString += (result.getStringParameter(G_Food2));
                            }

                            if (result.getStringParameter(F_Food).isEmpty() == false) {
                                if(result.getStringParameter(D_Food).isEmpty() == false || result.getStringParameter(E_Food).isEmpty() == false || result.getStringParameter(G_Food).isEmpty() == false)
                                    parameterString +=(",");
                                parameterString += (result.getStringParameter(F_Food));
                            }

                            if (result.getStringParameter(F_Food1).isEmpty() == false) {
                                parameterString +=(",");
                                parameterString += (result.getStringParameter(F_Food1));
                            }
                        }
                        if(result.getStringParameter(B_Food).isEmpty() == false) { //priority 3
                            if(number == 1)
                                parameterString += ("\n接著再吃");
                            else {
                                number = 1; //set flag
                                parameterString+=("建議您先享用");
                            }
                            parameterString += ( result.getStringParameter(B_Food));

                            if (result.getStringParameter(B_Food1).isEmpty() == false) {
                                parameterString +=(", ");
                                parameterString += (result.getStringParameter(B_Food1));
                            }
                            if (result.getStringParameter(B_Food2).isEmpty() == false) {
                                parameterString +=(", ");
                                parameterString += (result.getStringParameter(B_Food2));
                            }
                        }
                        if(result.getStringParameter(A_Food).isEmpty() == false || result.getStringParameter(O_Food).isEmpty() == false) { //priority 4
                            if(number == 1)
                                parameterString += ("\n接著再吃");
                            else {
                                number = 1; //set flag
                                parameterString+=("建議您先吃");
                            }
                            if(result.getStringParameter(A_Food).isEmpty() == false)
                                parameterString += (result.getStringParameter(A_Food));

                            if (result.getStringParameter(A_Food1).isEmpty() == false) {
                                parameterString +=(", ");
                                parameterString += (result.getStringParameter(A_Food1));
                            }
                            if (result.getStringParameter(A_Food2).isEmpty() == false) {
                                parameterString +=(", ");
                                parameterString += (result.getStringParameter(A_Food2));
                            }

                            if(result.getStringParameter(O_Food).isEmpty() == false) {
                                if(result.getStringParameter(A_Food).isEmpty() == false)
                                    parameterString += (", ");
                                parameterString += (result.getStringParameter(O_Food));
                                //int i;
                                for(i=0;i<foodCalList.size();i++){
                                    if(foodCalList.get(i).getChineseName().equals(result.getStringParameter(O_Food))){
                                        parameterString += ("它的熱量為"+foodCalList.get(i).getCalorie());
                                    }
                                }
                            }

                            if (result.getStringParameter(O_Food1).isEmpty() == false) {
                                parameterString +=(", ");
                                parameterString += (result.getStringParameter(O_Food1));
                            }
                            if (result.getStringParameter(O_Food2).isEmpty() == false) {
                                parameterString +=(", ");
                                parameterString += (result.getStringParameter(O_Food2));
                            }
                            if (result.getStringParameter(O_Food3).isEmpty() == false) {
                                parameterString +=(", ");
                                parameterString += (result.getStringParameter(O_Food3));
                            }
                        }
                        if(result.getStringParameter(I_Food).isEmpty() == false) { //priority 5
                            if(number == 1)
                                parameterString += ("\n接著再吃");
                            else {
                                number = 1; //set flag
                                parameterString+=("建議您先吃");
                            }
                            parameterString += ( result.getStringParameter(I_Food));

                            if (result.getStringParameter(I_Food1).isEmpty() == false) {
                                parameterString +=(", ");
                                parameterString += (result.getStringParameter(I_Food1));
                            }
                            if (result.getStringParameter(I_Food2).isEmpty() == false) {
                                parameterString +=(", ");
                                parameterString += (result.getStringParameter(I_Food2));
                            }
                        }
                        if(result.getStringParameter(L_Food).isEmpty() == false || result.getStringParameter(K_Food).isEmpty() == false) { //priority 6
                            if(number == 1) {
                                if(result.getStringParameter(L_Food).isEmpty() == false)
                                    parameterString += ("\n接著再喝");
                                else
                                    parameterString +=("\n接著再吃");
                            }
                            else {
                                number = 1; //set flag
                                parameterString+=("建議您先享用");
                            }
                            if(result.getStringParameter(L_Food).isEmpty() == false)
                                parameterString += (result.getStringParameter(L_Food));

                            if (result.getStringParameter(L_Food1).isEmpty() == false) {
                                parameterString +=(", ");
                                parameterString += (result.getStringParameter(L_Food1));
                            }
                            if (result.getStringParameter(L_Food2).isEmpty() == false) {
                                parameterString +=(", ");
                                parameterString += (result.getStringParameter(L_Food2));
                            }

                            if(result.getStringParameter(K_Food).isEmpty() == false) {
                                if(result.getStringParameter(L_Food).isEmpty() == false)
                                    parameterString += (",吃");
                                parameterString += (result.getStringParameter(K_Food));
                            }

                            if (result.getStringParameter(K_Food1).isEmpty() == false) {
                                parameterString +=(", ");
                                parameterString += (result.getStringParameter(K_Food1));
                            }
                            if (result.getStringParameter(K_Food2).isEmpty() == false) {
                                parameterString +=(", ");
                                parameterString += (result.getStringParameter(K_Food2));
                            }
                        }

                        if(number == 0){ //default conversation ( !! FOOD ISN'T IN THE DATABASE)
                            parameterString += ("很抱歉，此食物目前不存於資料庫中");
                        }

                        break;
                    case "get_absorb_calorie":

                        //get today's total calories
                        for( i=0;i<todayFoods.size();i++) {
                            total_absorb_calories += todayFoods.get(i).getCalorie();
                        }

                        idel_absorb_cal = sharedPreferences.getFloat("absorb",0);
//                        parameterString += "您今天吸收的熱量為"+ total_absorb_calories + "大卡\n";

                        if(idel_absorb_cal == 0 || total_absorb_calories == 0){
                            if(idel_absorb_cal == 0){
                                parameterString += "請前往“設定”填寫您的減重目標！";
                            }
                            else{
                                parameterString += "您今天尚未吃任何食物！";
                            }

                        }
                        else {
                            if (total_absorb_calories < idel_absorb_cal) {
                                parameterString += "您今天吸收的熱量為" + total_absorb_calories + "大卡\n";
                                float carolie_need = idel_absorb_cal - total_absorb_calories;
                                parameterString += "您距離每日理想熱量還有" + carolie_need + "" + "大卡\n";
                                parameterString += "建議補足每日需求熱量";

                            } else if (total_absorb_calories > idel_absorb_cal) {
                                parameterString += "您今天吸收的熱量為" + total_absorb_calories + "大卡\n";
                                parameterString += "您超過每日理想需求熱量" + ((total_absorb_calories - (double) idel_absorb_cal) + "")
                                        + "大卡\n";
                                parameterString += "建議多運動或減少每日進食量";
                            } else {
                                parameterString += "您今天攝取的熱量已足夠！ 建議多休息";
                            }
                        }
                        break;
                    case "get_consume_calorie":
                        //get today's total calories
                        for(i=0;i<todaySports.size();i++){
                            total_consume_calories += todaySports.get(i).getCalorie();
                        }

                        idel_consume_cal = sharedPreferences.getFloat("consume",0);

                        if(idel_consume_cal == 0 || total_consume_calories == 0){
                            if(idel_consume_cal == 0){
                                parameterString += "請前往“設定”填寫您的減重目標！";
                            }
                            else{
                                parameterString += "您今天尚未做任何運動！";
                            }
                        }
                        else{
                            if(total_consume_calories > idel_consume_cal){
                                parameterString += "您今天消耗" + total_consume_calories + "大卡\n";
                                parameterString += "建議補充膳食纖維高的食品以及多休息";
                            }
                            else if(total_consume_calories < idel_consume_cal){
                                parameterString += "您今天消耗" + total_consume_calories + "大卡\n";
                                parameterString += "距離每日理想消耗熱量還有" + (idel_consume_cal-total_consume_calories)
                                        + "大卡\n";
                                parameterString += "建議多運動以保持理想熱量消耗量";
                            }
                            else{
                                parameterString += "您今天達成每日理想每日消耗熱量！ 請繼續保持";
                            }
                        }
                        break;
                    case "get_tem":
                        parameterString += ("您的體溫為"+tem);
                        break;
                    case "get_water":
                        parameterString += ("您喝了"+water_drunk);
                    break;
                }
            }


        todayFoods.clear();
        todaySports.clear();
        return parameterString;
    }

    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.home) {
            Intent intent_home = new Intent();
            intent_home.setClass(ChatBotActivity.this, HomeActivity.class);
            Bundle bundle = new Bundle();
            bundle.putInt("BACK", 1);
            intent_home.putExtras(bundle);
            startActivity(intent_home);
            finish();
        } else if (id == R.id.food_list) {
            Intent intent_main = new Intent();
            intent_main.setClass(ChatBotActivity.this, MainActivity.class);
            startActivity(intent_main);
            finish();
            //Toast.makeText(this, "Open food list", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.Import) {
            selectImage();
            //Toast.makeText(this, "Import food", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.chat) {
            Intent intent_chat_bot = new Intent();
            intent_chat_bot.setClass(ChatBotActivity.this, ChatBotActivity.class);
            startActivity(intent_chat_bot);
            finish();
        } else if (id == R.id.new_calendar){
            Intent intent_new_calendar = new Intent();
            intent_new_calendar.setClass(ChatBotActivity.this, NewCalendarActivity.class);
            startActivity(intent_new_calendar);
            finish();
        } else if (id == R.id.blood_pressure){
            Intent intent_blood_pressure = new Intent();
            intent_blood_pressure.setClass(ChatBotActivity.this, MyBloodPressure.class);
            startActivity(intent_blood_pressure);
            finish();
        } else if (id == R.id.temp_record){
            Intent intent_temp_record = new Intent();
            intent_temp_record.setClass(ChatBotActivity.this, MyTemperatureRecord.class);
            startActivity(intent_temp_record);
            finish();
        } else if (id == R.id.water_record){
            Intent intent_water_record = new Intent();
            intent_water_record.setClass(ChatBotActivity.this, DrinkWaterDiary.class);
            startActivity(intent_water_record);
            finish();
        } else if (id == R.id.message) {
            Intent intent_message = new Intent();
            intent_message.setClass(ChatBotActivity.this, MessageActivity.class);
            startActivity(intent_message);
            finish();
            //Toast.makeText(this, "Send message", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.mail){
            Intent intent_mail = new Intent();
            intent_mail.setClass(ChatBotActivity.this, MailActivity.class);
            startActivity(intent_mail);
            finish();
        } else if (id == R.id.setting_list) {
            Intent intent_setting = new Intent();
            intent_setting.setClass(ChatBotActivity.this, SettingsActivity.class);
            startActivity(intent_setting);
            finish();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onError(final AIError error) {

    }

    @Override
    public void onListeningStarted() {
    }

    @Override
    public void onListeningCanceled() {
        btnRecord.setImageResource(R.drawable.ic_mic_black_48dp);
    }

    @Override
    public void onListeningFinished() {
        btnRecord.setImageResource(R.drawable.ic_mic_black_48dp);
    }

    @Override
    public void onAudioLevel(final float level) {
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void selectImage() {
        final CharSequence[] items = {"照相", "從相簿中選取", "取消"};
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("新增食物");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int index) {
                if (items[index].equals("照相")) {
                    if (activityIndex == ChATBOT_ACTIVITY) {
                        /*Intent intent_camera = new Intent("com.example.nthucs.prototype.TAKE_PICT");
                        activity.startActivityForResult(intent_camera, SCAN_FOOD);*/
                        Intent result = new Intent();
                        result.putExtra(FROM_CAMERA, SCAN_FOOD);
                        result.setClass(activity, MainActivity.class);
                        activity.startActivity(result);
                        activity.finish();
                    } /*else {
                        // back to mail activity
                        Intent result = new Intent();
                        result.putExtra(FROM_CAMERA, SCAN_FOOD);
                        result.setClass(activity, MainActivity.class);
                        activity.startActivity(result);
                        activity.finish();
                    }*/
                } else if (items[index].equals("從相簿中選取")) {
                    if (activityIndex == ChATBOT_ACTIVITY) {
                        /*Intent intent_gallery = new Intent("com.example.nthucs.prototype.TAKE_PHOTO");
                        activity.startActivityForResult(intent_gallery, TAKE_PHOTO);*/
                        Intent result = new Intent();
                        result.putExtra(FROM_GALLERY, TAKE_PHOTO);
                        result.setClass(activity, MainActivity.class);
                        activity.startActivity(result);
                        activity.finish();
                    } /*else {
                        // back to mail activity
                        Intent result = new Intent();
                        result.putExtra(FROM_GALLERY, TAKE_PHOTO);
                        result.setClass(activity, MainActivity.class);
                        activity.startActivity(result);
                        activity.finish();
                    }*/
                } else if (items[index].equals("取消")) {
                    dialog.dismiss();
//                    Intent intent = new Intent();
//                    intent.setClass(ChatBotActivity.this, MailActivity.class);
//                    startActivity(intent);
                }
            }
        });
        builder.show();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState){
        //savedInstanceState.putString();
        super.onSaveInstanceState(savedInstanceState);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.chat_bot, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if (id == R.id.language_change){
            if(flag == 0){
                config = new AIConfiguration("3f5da70a97c44731b8d7ac44b6acb7ef",
                        AIConfiguration.SupportedLanguages.English,
                        AIConfiguration.RecognitionEngine.System);
                aiService = AIService.getService(this, config);
                aiService.setListener(this);
                flag = 1;
                //Instruction (chatbot使用說明 => 讓user清楚知道目前聊天機器人有甚麼功能)
                Message eng_message = new Message();
                eng_message.setId("4");
                eng_message.setMessage("Welcome "+LoginActivity.facebookName + "\n"+
                        "Now chatbot can support\n" +
                        "1.Ask info about height、weight、bmi\n" +
                        "2.Ask info about eating order\n" +
                        "3.Ask info about pressure\n"+
                        "4.Ask info about calories");
                messageArrayList.clear();
                messageArrayList.add(eng_message);
                mAdapter.notifyDataSetChanged();
                Toast.makeText(ChatBotActivity.this, "English Chat Bot", Toast.LENGTH_SHORT).show();
            }
            else{
                config = new AIConfiguration("a772958d63a149b39bf9f11cfad29889",
                        AIConfiguration.SupportedLanguages.ChineseTaiwan,
                        AIConfiguration.RecognitionEngine.System);
                aiService = AIService.getService(this, config);
                aiService.setListener(this);
                flag = 0;
                //Instruction (chatbot使用說明 => 讓user清楚知道目前聊天機器人有甚麼功能)
                Message ini_message = new Message();
                ini_message.setId("3");
                ini_message.setMessage("歡迎"+LoginActivity.facebookName +" 本聊天機器人目前支援功能有\n" +
                        "1.詢問身高、體重、bmi\n" +
                        "2.詢問飲食順序\n" +
                        "3.詢問血壓/脈搏狀況\n"+
                        "4.查詢目前消耗/吸收熱量");
                messageArrayList.clear();
                messageArrayList.add(ini_message);
                mAdapter.notifyDataSetChanged();
                Toast.makeText(ChatBotActivity.this, "中文管家", Toast.LENGTH_SHORT).show();
            }
        }

        return super.onOptionsItemSelected(item);
    }


}
