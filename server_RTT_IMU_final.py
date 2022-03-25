from flask import Flask,request
import csv
import math

AP1_Mac = "b0:e4:d5:39:26:89"
AP2_Mac = "cc:f4:11:8b:29:4d"
AP3_Mac = "b0:e4:d5:01:26:f5"
AP4_Mac = "b0:e4:d5:5f:f2:ad"
AP5_Mac = "b0:e4:d5:96:3b:95"
AP6_Mac = "b0:e4:d5:91:ba:5d"

def tr(x):
    d = math.sqrt(x ** 2 - 1.45 * 1.45)
    return d
x_l = [7.21, 13.26, 0, 19.2, 26.08, 29.18]
x_l = [48.12-i for i in x_l]
y_l = [-1.7, 0, 0, -1.46, -2.35, 0]
y_l = [11.45-i for i in y_l]
def must_two_point(x_1i, y_1i, d_1i, x_2i, y_2i, d_2i):  # 防止两圆无交点
    d = math.sqrt((x_1i - x_2i) ** 2 + (y_1i - y_2i) ** 2)  # 计算两圆心距离

    if d > d_1i + d_2i or d < abs(d_1i - d_2i):  # 防止两个圆无交点
        if d > d_1i + d_2i:  # 相离
            while d > d_1i + d_2i:
                d_1i *= 1.00001
                d_2i *= 1.00001
                d = math.sqrt((x_1i - x_2i) ** 2 + (y_1i - y_2i) ** 2)  # 同时增大
            return d, d_1i, d_2i
        else:  # 内含
            while d < abs(d_1i - d_2i):
                if d_1i > d_2i:
                    d_1i *= 0.9999
                    d = math.sqrt((x_1i - x_2i) ** 2 + (y_1i - y_2i) ** 2)
                else:
                    d_2i *= 0.9999
                    d = math.sqrt((x_1i - x_2i) ** 2 +
                                  (y_1i - y_2i) ** 2)  # 将大圆缩小

            return d, d_1i, d_2i

    else:
        return d, d_1i, d_2i


def get_intersections(x_i1, y_i1, d_i1, x_i2, y_i2, d_i2):
    # circle 1: (x1, y1), radius d1
    # circle 2: (x2, y2), radius d2
    [d, d_i1, d_i2] = must_two_point(
        x_i1, y_i1, d_i1, x_i2, y_i2, d_i2)  # 通过输入的点获取距离

    a = (d_i1 ** 2 - d_i2 ** 2 + d ** 2) / (2 * d)
    h = math.sqrt(abs(d_i1 ** 2 - a ** 2))


    x = x_i1 + a * (x_i2 - x_i1) / d
    y = y_i1 + a * (y_i2 - y_i1) / d
    i_x1 = x + h * (y_i2 - y_i1) / d
    i_y1 = y - h * (x_i2 - x_i1) / d
    i_x2 = x - h * (y_i2 - y_i1) / d
    i_y2 = y + h * (x_i2 - x_i1) / d  # 两个圆相交，有两个或一个交点。获取交点坐标
    return [i_x1, i_y1, i_x2, i_y2]



def pick_point(x_1, y_1, d_1, x_2, y_2, d_2, x_3, y_3, d_3):
    r1 = get_intersections(x_1, y_1, d_1, x_2, y_2, d_2)  # 获得两个交点

    len1 = math.sqrt((r1[0] - x_3) ** 2 + (r1[1] - y_3) ** 2)
    len2 = math.sqrt((r1[2] - x_3) ** 2 + (r1[3] - y_3) ** 2)  # 计算两点到第三个圆的圆心距离
    if len1 < d_3:
        len1_new = d_3 - len1
    else:
        len1_new = len1 - d_3
    if len2 < d_3:
        len2_new = d_3 - len2
    else:
        len2_new = len2 - d_3  # 计算交点到第三个圆边的距离
    if len1_new < len2_new:
        x_s = r1[0]
        y_s = r1[1]
    else:
        x_s = r1[2]
        y_s = r1[3]  # 选取离第三个圆边较短的点

    return [x_s, y_s]

def sort(lst):
    n=len(lst)
    if n<=1:
        return lst
    for i in range (0,n):
        for j in range(0,n-i-1):
            if lst[j][0]>lst[j+1][0]:
                (lst[j],lst[j+1])=(lst[j+1],lst[j])
    return lst

with open('rtt.csv','w') as csv_file:
    writer = csv.writer(csv_file)
    writer.writerow(['Timestamp','RTT_result'])
    
with open('imu.csv','w') as csv_file:
    writer = csv.writer(csv_file)
    writer.writerow(['Timestamp','Accx','Accy','Accz','Gyrox','Gyroy','Gyroz','Azimuth','Pitch','Roll'])

temp_list = []
app = Flask(__name__)

@app.route('/server', methods=['GET','POST'])
def server():
    global temp_list
    r = request.form
    data = r.to_dict(flat=False)
    
    if data['Flag'][0] == 'IMU':
        time = int(str(data['Timestamp'])[2:-2])
        accx = float(data['Accx'][0])
        accy = float(data['Accy'][0])
        accz = float(data['Accz'][0])
        gyrox = float(data['Gyrox'][0])
        gyroy = float(data['Gyroy'][0])
        gyroz = float(data['Gyroz'][0])
        azimuth = float(data['Azimuth'][0])
        pitch = float(data['Pitch'][0])
        roll = float(data['Roll'][0])
        temp = [time,accx,accy,accz,gyrox,gyroy,gyroz,azimuth,pitch,roll]
        if len(temp_list) == 10:
            temp_list = temp_list[1::]
            temp_list.append(temp)
        else:
            temp_list.append(temp)
        sorted_list = sort(temp_list)
        temp_list = sorted_list
        if len(sorted_list) == 10:
            with open('imu.csv','a+') as csv_file:
                writer = csv.writer(csv_file)
                writer.writerow(sorted_list[0])
        return("IMUOK")
    
    elif data['Flag'][0] == 'RTT':
        time = int(str(data['Timestamp'])[2:-2])
        RTT_Result = data['RTT_Result']

        punctuation = ["'", '[', ']', ' ']

        for i in punctuation:

            try:
                RTT_Result[0] = RTT_Result[0].replace(i, "")
                RTT_Result[4] = RTT_Result[4].replace(i, "")
                RTT_Result[8] = RTT_Result[8].replace(i, "")
                RTT_Result[12] = RTT_Result[12].replace(i, "")
                RTT_Result[16] = RTT_Result[16].replace(i, "")
                RTT_Result[20] = RTT_Result[20].replace(i, "")
                #RTT_Result[23] = RTT_Result[23].replace(i,"")

            except:
                continue

        RTT_Result = RTT_Result[0]
        RTT_Result = RTT_Result.split(",")
        #print(RTT_Result)

        try:
            d1 = float(RTT_Result[RTT_Result.index(AP1_Mac) + 1]) / 1000
            d1 = tr(d1)
            

        except:
            d1 = 0
            



        try:
            d2 = float(RTT_Result[RTT_Result.index(AP2_Mac) + 1]) / 1000
            d2 = tr(d2)
           
        except:
            d2 = 0
           

        try:
            d3 = float(RTT_Result[RTT_Result.index(AP3_Mac) + 1]) / 1000
            d3 = tr(d3)
            
        except:
            d3 = 0
           

        try:
            d4 = float(RTT_Result[RTT_Result.index(AP4_Mac) + 1]) / 1000
            d4 = tr(d4)
           
        except:
            d4 = 0
            

        try:
            d5 = float(RTT_Result[RTT_Result.index(AP5_Mac) + 1]) / 1000
            d5 = tr(d5)
           
        except:
            d5 = 0
            

        try:
            d6 = float(RTT_Result[RTT_Result.index(AP6_Mac) + 1]) / 1000
            d6 = tr(d6)
            

        except:
            d6 = 0
            


        d_list = [d1, d2, d3, d4, d5, d6]
        dd_list = [d1, d2, d3, d4, d5, d6]
        # i = 0
        # while i < len(d_list):
        #     if d_list[i] < 3:
        #         del(d_list[i])
        #         i -= 1
        #     i += 1
        d_list.sort(key=None, reverse=False)
        d_list = list(set(d_list))
        #print(d_list)
        d1 = d_list[0]
        p = dd_list.index(d1)

        x1 = x_l[p]
        y1 = y_l[p]

        d2 = d_list[1]
        w = dd_list.index(d2)
        x2 = x_l[w]
        y2 = y_l[w]

        d3 = d_list[2]
        z = dd_list.index(d3)
        x3 = x_l[z]
        y3 = y_l[z]
        [t11, t12] = pick_point(x1, y1, d1, x2, y2, d2, x3, y3, d3)
        [t21, t22] = pick_point(x1, y1, d1, x3, y3, d3, x2, y2, d2)
        [t31, t32] = pick_point(x2, y2, d2, x3, y3, d3, x1, y1, d1)  # 选择三个点
        point_x = (t11 + t21 + t31) / 3
        point_y = (t12 + t22 + t32) / 3



        
        with open('rtt.csv','a+') as csv_file:
            writer = csv.writer(csv_file)
            writer.writerow([time,RTT_Result])
            #return("RTTOK")
            #print(point_x)
            #print(point_y)
            temp = str(point_x)+" "+str(point_y)
            return(temp)

if __name__ == '__main__':
    app.run(host='0.0.0.0')