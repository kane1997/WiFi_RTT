from flask import Flask,request
import csv

with open('rtt.csv','w') as csv_file:
    writer = csv.writer(csv_file)
    writer.writerow(['Timestamp','IMU_result'])
    
with open('imu.csv','w') as csv_file:
    writer = csv.writer(csv_file)
    writer.writerow(['Timestamp','Accx','Accy','Accz','Grox','Groy','Groz','Magx','Magy','Magz'])


app = Flask(__name__)

@app.route('/server', methods=['GET','POST'])
def server():
    r = request.form
    data = r.to_dict(flat=False)
    
    if data['Flag'][0] == 'IMU':
        time = int(str(data['Timestamp'])[2:-2])
        accx = data['accx']
        accy = data['accy']
        accz = data['accz']
        gyrox = data['gyrox']
        gyroy = data['gyroy']
        gyroz = data['gyroz']
        magx = data['magx']
        magy = data['magy']
        magz = data['magz']
        with open('imu.csv','a+') as csv_file:
            writer = csv.writer(csv_file)
            writer.writerow([time,accx,accy,accz,gyrox,gyroy,gyroz,magx,magy,magz])
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