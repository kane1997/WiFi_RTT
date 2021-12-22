from flask import Flask,request
import csv

with open('rtt.csv','w') as csv_file:
        writer = csv.writer(csv_file)
        writer.writerow(['Mac_1','Distance_1','RSSI_1','Mac_2','Distance_2','RSSI_2','Mac_3','Distance_3','RSSI_3'])
        
app = Flask(__name__)
 
@app.route('/server', methods=['GET','POST'])
def server():
    r = request.form
    data = r.to_dict(flat=False)
    Mac_1 = str(data['Mac_1'])
    Distance_1 = int(str(data['Distance_1'][0]))
    RSSI_1 = int(str(data['RSSI_1'][0]))

    Mac_2 = str(data['Mac_2'])
    Distance_2 = int(str(data['Distance_2'][0]))
    RSSI_2 = int(str(data['RSSI_2'][0]))
    
    Mac_3 = str(data['Mac_3'])
    Distance_3 = int(str(data['Distance_3'][0]))
    RSSI_3 = int(str(data['RSSI_3'][0]))
    RTT_data = [Mac_1,Distance_1,RSSI_1,Mac_2,Distance_2,RSSI_2,Mac_3,Distance_3,RSSI_3]
    with open('rtt.csv','a+') as csv_file:
        writer = csv.writer(csv_file)
        writer.writerow(RTT_data)
    return("ok")
 
if __name__ == '__main__':
    app.run(host='0.0.0.0')
