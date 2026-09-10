package com.jisuanyusuiji.toolbox.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

const val AGREEMENT_TITLE = "《小温工具箱用户服务协议》"

const val AGREEMENT_TEXT = """欢迎使用“小温工具箱”（以下简称“本软件”）。在使用本软件前，请您（以下简称“用户”）仔细阅读并充分理解本协议全部内容。您勾选同意并点击“同意并继续”，即视为已阅读、理解并同意本协议全部条款；如不同意，请点击“不同意并退出”并停止使用本软件。

一、服务说明
1. 本软件是一款运行于用户设备本地的离线工具箱，提供随机工具、计算与转换、文本与编码、图片与视频处理、图鉴查询等功能。
2. 本软件不提供联网服务，不收集、不上传用户的任何数据、文件或个人信息。
3. 本软件按“现状”提供，开发者不对服务的绝对无中断、无错误作出保证。

二、使用规则
1. 用户应遵守中华人民共和国法律法规，不得利用本软件从事赌博、诈骗、侵权、传播违法信息等行为。
2. 随机类工具的结果仅供娱乐与决策参考，不构成任何形式的承诺或保证，用户不得将随机结果用于违法用途。
3. 金融、房贷、利息、BMI、油耗等计算结果仅供参考，不构成投资、医疗、法律等专业建议，用户应自行核实并承担决策后果。
4. 用户对自己导入、处理和导出的文件负责，请在操作前自行备份原始文件。

三、数据与隐私
1. 本软件所有配置、历史、模板等数据均保存在用户设备本地。
2. 卸载本软件或清除应用数据将导致本地数据丢失，请自行做好备份。
3. 本软件不申请网络权限，不存在数据上传行为。使用手电筒、镜子时需申请相机权限，使用录音机时需申请麦克风权限，相关功能均在本机完成，不会上传或分享。
4. 用户导入的车标图片、录音、备忘录等数据仅保存在本机应用目录中，卸载后会被删除。

四、知识产权
1. 本软件的程序、界面、文字等知识产权归开发者所有。
2. 用户通过本软件处理的自身文件，其权利仍归用户或原权利人所有。
3. 未经开发者许可，不得对本软件进行反向工程、恶意修改、非法传播或用于商业用途。

五、免责声明
1. 因用户自身误操作、设备故障、系统升级、文件损坏等原因造成的任何损失，开发者不承担赔偿责任。
2. 因不可抗力或非开发者原因导致的服务中断或数据丢失，开发者不承担赔偿责任。
3. 在法律允许的最大范围内，开发者对本软件不承担任何间接、附带或惩罚性赔偿责任。

六、协议变更
1. 开发者有权根据法律法规或功能调整需要更新本协议，更新后将在软件内提示。
2. 协议更新后，用户继续使用本软件即视为接受更新后的协议；如不同意，请停止使用。

七、法律适用与争议解决
1. 本协议的订立、效力、解释与争议解决均适用中华人民共和国法律。
2. 因本协议产生的争议，双方应友好协商解决；协商不成的，可向开发者所在地有管辖权的人民法院提起诉讼。

八、解释权
1. 在法律允许的范围内，本协议的最终解释权归开发者所有。
2. 如本条或本协议任何条款与法律法规相冲突，以法律法规为准；该条款不影响用户依法享有的法定权利。

九、其他
1. 本协议自用户点击“同意并继续”之日起生效。
2. 如对本协议有任何疑问，请通过您获取本软件的渠道联系开发者。

更新日期：2026 年 9 月"""

/** 首次启动强制同意页：不勾选同意无法进入 App。 */
@Composable
fun UserAgreementScreen(onAgree: () -> Unit, onDecline: () -> Unit) {
    var checked by remember { mutableStateOf(false) }

    BackHandler { onDecline() }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Text(
                AGREEMENT_TITLE,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "请仔细阅读以下条款，勾选同意后方可使用本软件。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    AGREEMENT_TEXT,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.35f
                )
                Spacer(Modifier.height(16.dp))
            }
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = checked, onCheckedChange = { checked = it })
                Text("我已阅读并同意本协议全部条款", style = MaterialTheme.typography.bodyMedium)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f)
                ) { Text("不同意并退出") }
                Button(
                    onClick = onAgree,
                    enabled = checked,
                    modifier = Modifier.weight(1f)
                ) { Text("同意并继续") }
            }
        }
    }
}

/** 设置页里查看协议用的对话框。 */
@Composable
fun AgreementDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AGREEMENT_TITLE) },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(AGREEMENT_TEXT, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}
