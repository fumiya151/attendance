document.addEventListener('DOMContentLoaded', () => {
    let selectedUserName = '';
    let selectedUserCode = '';

    const buttons = {
        '出勤': document.querySelector('.is-in'),
        '退勤': document.querySelector('.is-out'),
        '休憩開始': document.querySelector('.is-break-start'),
        '休憩終了': document.querySelector('.is-break-end')
    };

    // リアルタイム時計更新
    function updateTime() {
        const now = new Date();
        document.getElementById('current-time').textContent = now.toLocaleTimeString('ja-JP', { hour: '2-digit', minute: '2-digit' });
        document.getElementById('current-date').textContent = now.toLocaleDateString('ja-JP', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'short' });
    }
    setInterval(updateTime, 1000);
    updateTime();

    // 従業員リストの生成 (APIから取得)
    async function fetchAndRenderEmployees() {
        try {
            const response = await fetch('/api/employees');
            
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            const employees = await response.json();

            const listElement = document.getElementById('employee-list');
            listElement.innerHTML = ''; // Clear existing list
            employees.forEach(emp => {
                const item = document.createElement('div');
                item.className = 'employee-item';
                item.textContent = emp.name;
                item.setAttribute('data-code', emp.employeeId);
                item.onclick = () => selectEmployee(emp.name, emp.employeeId, item);
                listElement.appendChild(item);
            });
        } catch (error) {
            console.error('Failed to fetch employees:', error);
            const listElement = document.getElementById('employee-list');
            listElement.innerHTML = '<p style="color: red;">従業員リストの読み込みに失敗しました。</p>';
        }
    }

    // ボタンの状態を更新する
    async function updateButtonStates(employeeId) {
        // まずすべてのボタンを非活性化
        Object.values(buttons).forEach(btn => btn.disabled = true);

        if (!employeeId) {
            return; // 従業員が選択されていなければ非活性のまま
        }

        try {
            const encodedEmployeeId = encodeURIComponent(employeeId);
            const response = await fetch(`/api/attendance/next-available/${encodedEmployeeId}`);
            
            if (response.ok) {
                const availableTypes = await response.json();
                // 有効な打刻種別のみ有効化
                availableTypes.forEach(type => {
                    if (buttons[type]) {
                        buttons[type].disabled = false;
                    }
                });
            }
        } catch (error) {
            console.error('Error fetching next available stamp types:', error);
        }
    }

    // 従業員選択時の処理
    window.selectEmployee = function (name, code, element) {
        selectedUserName = name;
        selectedUserCode = code;
        document.getElementById('selected-name').textContent = name;

        // 選択状態のハイライト
        document.querySelectorAll('.employee-item').forEach(item => {
            item.classList.remove('is-selected');
        });
        element.classList.add('is-selected');

        document.getElementById('message-area').textContent = `${name}さんを選択しました。打刻ボタンを押してください。`;
        document.getElementById('message-area').className = 'message-box is-info';

        document.querySelector('.selected-user-info .prompt').style.display = 'none';
        updateButtonStates(code); // ボタンの状態を更新
    }

    // 打刻処理
    window.stamp = async function (type) {
        const messageArea = document.getElementById('message-area');

        if (!selectedUserCode) {
            messageArea.textContent = '⚠️ まずリストから自分の名前を選択してください！';
            messageArea.className = 'message-box is-error';
            return;
        }

        const attendanceData = {
            employeeId: selectedUserCode,
            name: selectedUserName, // Controllerのメッセージ作成用に残す
            stampType: type
        };

        try {
            // 打刻対象者が操作者IDを兼ねる
            const operatorId = selectedUserCode; 
            const response = await fetch('/api/attendance/stamp', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Operator-Id': operatorId 
                },
                body: JSON.stringify(attendanceData),
            });

            const responseText = await response.text();

            if (response.ok) {
                messageArea.textContent = `✅ ${responseText}`;
                messageArea.className = 'message-box is-success';
                updateButtonStates(selectedUserCode); // 打刻成功後にボタン状態を更新
            } else {
                // JSONエラーレスポンスの処理を強化
                try {
                    const errorJson = JSON.parse(responseText);
                    throw new Error(errorJson.message || `サーバーエラーが発生しました (Status: ${response.status})`);
                } catch {
                    throw new Error(responseText || `サーバーでエラーが発生しました (Status: ${response.status})`);
                }
            }

        } catch (error) {
            console.error('Stamp error:', error);
            messageArea.textContent = `❌ 打刻に失敗しました: ${error.message}`;
            messageArea.className = 'message-box is-error';
        }
    }

    fetchAndRenderEmployees();
    updateButtonStates(null); // 初期状態では出勤以外は非活性
});