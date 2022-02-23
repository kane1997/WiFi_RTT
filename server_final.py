from flask import Flask,request
import csv

with open('rtt.csv','w') as csv_file:
    writer = csv.writer(csv_file)
    writer.writerow(['Timestamp','RTT_result'])
    
with open('imu.csv','w') as csv_file:
    writer = csv.writer(csv_file)
    writer.writerow(['Timestamp','IMU_result'])


app = Flask(__name__)

@app.route('/server', methods=['GET','POST'])
def server():
    r = request.form
    data = r.to_dict(flat=False)
    #print(data['Flag'])
    
    if data['Flag'][0] == 'IMU':
        #print(1)
        time = int(str(data['Timestamp'])[2:-3])
        IMU_Result = data['IMU_Result']
        with open('imu.csv','a+') as csv_file:
            writer = csv.writer(csv_file)
            writer.writerow([time,IMU_Result])
            return("IMUOK")
    elif data['Flag'][0] == 'RTT':
        #print(2)
        time = int(str(data['Timestamp'])[2:-3])
        RTT_Result = data['RTT_Result']
        with open('rtt.csv','a+') as csv_file:
            writer = csv.writer(csv_file)
            writer.writerow([time,RTT_Result])
            return("RTTOK")

if __name__ == '__main__':
    app.run(host='0.0.0.0')