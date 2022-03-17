from flask import Flask,request
import csv

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
    writer.writerow(['Timestamp','RTT_Result'])
    
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
        with open('rtt.csv','a+') as csv_file:
            writer = csv.writer(csv_file)
            writer.writerow([time,RTT_Result])
            return("RTTOK")

if __name__ == '__main__':
    app.run(host='0.0.0.0')