document.addEventListener('DOMContentLoaded', function () {
    const calculateBtn = document.getElementById('calculate-btn');
    const tableBody = document.getElementById('payroll-table-body');

    calculateBtn.addEventListener('click', fetchAndDisplayPayroll);

    function fetchAndDisplayPayroll() {
        // ローディング表示
        tableBody.innerHTML = '<tr><td colspan="4">計算中...</td></tr>';

        fetch('/api/payroll/calculate')
            .then(response => {
                if (!response.ok) {
                    throw new Error('Network response was not ok');
                }
                return response.json();
            })
            .then(data => {
                tableBody.innerHTML = ''; // テーブルをクリア
                if (data.length === 0) {
                    tableBody.innerHTML = '<tr><td colspan="4">計算対象の従業員が見つかりません。従業員に時給が設定されているか確認してください。</td></tr>';
                    return;
                }

                data.forEach(payroll => {
                    const row = document.createElement('tr');
                    row.innerHTML = `
                        <td>${payroll.employeeId}</td>
                        <td>${payroll.employeeName}</td>
                        <td>${payroll.totalHours.toFixed(2)}</td>
                        <td>${Math.round(payroll.calculatedSalary).toLocaleString()}</td>
                    `;
                    tableBody.appendChild(row);
                });
            })
            .catch(error => {
                console.error('Error fetching payroll data:', error);
                tableBody.innerHTML = '<tr><td colspan="4">データの取得に失敗しました。サーバーログを確認してください。</td></tr>';
            });
    }
    
    // 初期表示時に計算を実行
    fetchAndDisplayPayroll();
});
