from flask import Flask,request
import csv

with open('rtt.csv','w') as csv_file:
    writer = csv.writer(csv_file)
    writer.writerow(['Timestamp','RTT_result','IMU_result'])

app = Flask(__name__)

@app.route('/server', methods=['GET','POST'])
def server():
    r = request.form
    data = r.to_dict(flat=False)

    time = int(str(data['Timestamp'])[2:-3])
    RTT_Result = data['RTT_Result']
    IMU_Result = data['IMU_Result']
 
    print(time)

    with open('rtt.csv','a+') as csv_file:
        writer = csv.writer(csv_file)
        writer.writerow([time,RTT_Result,IMU_Result])
    return("ok")

if __name__ == '__main__':
    app.run(host='0.0.0.0')