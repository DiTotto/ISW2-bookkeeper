import csv
import time
from os import listdir
from os.path import isfile, join
import numpy
from configs import files_path, include_types, exclude_strings
from models import get_index_value
delimiter = ';'
processed_csv_file_data = []
processed_csv_file_data_normalized = []
mypath = files_path
onlyfiles = [f for f in listdir(mypath) if isfile(join(mypath, f))]
t = time.time()
total_data = []

for file_num, filename in enumerate(onlyfiles):
    AUC_MAPPINGS_TP = numpy.array([0 for j in range(0, 10001)])
    AUC_MAPPINGS_TN = numpy.array([0 for j in range(0, 10001)])
    AUC_MAPPINGS_FP = numpy.array([0 for j in range(0, 10001)])
    AUC_MAPPINGS_FN = numpy.array([0 for j in range(0, 10001)])

    extension = filename.split(".")[1]
    if extension not in include_types:
        continue
    exclude_check = False
    for excluder in exclude_strings:
        if excluder in filename:
            exclude_check = True
            break
    if exclude_check:
        continue

    file = open(mypath + filename)
    total_line_of_code = 0  # total lines of code
    total_nr_no = 0  # total line of false rows (used for normalization of IFA)
    """
    File reading and initial transformation
    """
    # data_prediction = []
    for row_index, row in enumerate(file):
        if row_index == 0:
            continue
        lst = row.split(delimiter)  # delimiter
        id, size, prediction, actual = lst[0], int(lst[1]) if lst[1] else 0, float(lst[2]), True if lst[3].strip().upper() == 'YES' else False
        index = get_index_value(True, actual)
        left = int(prediction * 1000 * 10)
        if prediction - left / 10000 != 0:
            left -= 1
        right = left + 1
        if index == "TN":
            AUC_MAPPINGS_TN[0: left + 1] += 1
        elif index == "TP":
            AUC_MAPPINGS_TP[0: left + 1] += 1
        elif index == "FN":
            AUC_MAPPINGS_FN[0: left + 1] += 1
        elif index == "FP":
            AUC_MAPPINGS_FP[0: left + 1] += 1
        index = get_index_value(False, actual)

        if index == "TN":
            AUC_MAPPINGS_TN[right:] += 1
        elif index == "TP":
            AUC_MAPPINGS_TP[right:] += 1
        elif index == "FN":
            AUC_MAPPINGS_FN[right:] += 1
        elif index == "FP":
            AUC_MAPPINGS_FP[right:] += 1

    print("Processing file # ", file_num + 1, filename, file_num)

    output_mappings = [[1000, 0, 10], [100, 0, 100], [10, 0, 1000], [1, 0, 10000]]
    index = 13
    while index < 10000:
        output_mappings.append([int(10000 / index), 0, index])
        index = round(index * 1.25)
    output_mappings = sorted(output_mappings, key=lambda x: x[2])
    for index in range(0, len(AUC_MAPPINGS_TN[0:-1])):
        tpc = AUC_MAPPINGS_TP[index]
        tnc = AUC_MAPPINGS_TN[index]
        fpc = AUC_MAPPINGS_FP[index]
        fnc = AUC_MAPPINGS_FN[index]
        tpr = tpc / (tpc + fnc) if tpc + fnc else 0
        fpr = fpc / (fpc + tnc) if fpc + tnc else 0

        for output_mapping in output_mappings:
            divider = output_mapping[0]
            if index % divider == 0:
                ftnc = AUC_MAPPINGS_TN[index + divider] if index + divider < len(AUC_MAPPINGS_TN) else 0
                ffpc = AUC_MAPPINGS_FP[index + divider] if index + divider < len(AUC_MAPPINGS_FP) else 0

                future_fpr = ffpc / (ffpc + ftnc) if ffpc + ftnc else 0
                AUC_DELTA = (fpr - future_fpr) * tpr
                output_mapping[1] += AUC_DELTA
    row_data = f"{filename}; ;"
    for output_mapping in output_mappings:
        row_data += f"{output_mapping[1]};"
    total_data.append(row_data)

output_file_name = "auc_output_t.csv"  # file name
f = open(output_file_name, "w")

header = f"Filename;WEKA;"
for name_header in output_mappings:
    header += f"AUC{name_header[2]};"
writer = csv.writer(f)
writer.writerow(header.split(";"))
writer.writerows([x.split(";") for x in total_data])
f.close()
print(time.time() - t)

import pandas as pd

pd.read_csv('auc_output_t.csv', header=None).T.to_csv('auc_output_t.csv', header=False, index=False)
