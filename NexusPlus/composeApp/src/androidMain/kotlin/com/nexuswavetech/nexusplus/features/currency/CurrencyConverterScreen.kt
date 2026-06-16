package com.nexuswavetech.nexusplus.features.currency

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private data class Currency(val code: String, val name: String, val symbol: String, val flag: String)

private val CURRENCIES = listOf(
    Currency("USD","US Dollar","$","🇺🇸"), Currency("EUR","Euro","€","🇪🇺"),
    Currency("GBP","British Pound","£","🇬🇧"), Currency("INR","Indian Rupee","₹","🇮🇳"),
    Currency("JPY","Japanese Yen","¥","🇯🇵"), Currency("CNY","Chinese Yuan","¥","🇨🇳"),
    Currency("AED","UAE Dirham","د.إ","🇦🇪"), Currency("SGD","Singapore Dollar","S$","🇸🇬"),
    Currency("CAD","Canadian Dollar","C$","🇨🇦"), Currency("AUD","Australian Dollar","A$","🇦🇺"),
    Currency("CHF","Swiss Franc","Fr","🇨🇭"), Currency("SAR","Saudi Riyal","﷼","🇸🇦"),
    Currency("MYR","Malaysian Ringgit","RM","🇲🇾"), Currency("BRL","Brazilian Real","R$","🇧🇷"),
    Currency("MXN","Mexican Peso","$","🇲🇽"), Currency("KRW","South Korean Won","₩","🇰🇷"),
    Currency("ZAR","South African Rand","R","🇿🇦"), Currency("TRY","Turkish Lira","₺","🇹🇷"),
    Currency("THB","Thai Baht","฿","🇹🇭"), Currency("IDR","Indonesian Rupiah","Rp","🇮🇩"),
    Currency("PKR","Pakistani Rupee","₨","🇵🇰"), Currency("BDT","Bangladeshi Taka","৳","🇧🇩"),
    Currency("NZD","New Zealand Dollar","NZ$","🇳🇿"), Currency("HKD","Hong Kong Dollar","HK$","🇭🇰"),
    Currency("SEK","Swedish Krona","kr","🇸🇪"), Currency("NOK","Norwegian Krone","kr","🇳🇴"),
    Currency("DKK","Danish Krone","kr","🇩🇰"), Currency("RUB","Russian Ruble","₽","🇷🇺"),
    Currency("EGP","Egyptian Pound","£","🇪🇬"), Currency("NGN","Nigerian Naira","₦","🇳🇬"),
    Currency("QAR","Qatari Riyal","ر.ق","🇶🇦"), Currency("KWD","Kuwaiti Dinar","د.ك","🇰🇼"),
    Currency("OMR","Omani Rial","ر.ع","🇴🇲"), Currency("LKR","Sri Lankan Rupee","₨","🇱🇰"),
    Currency("NPR","Nepalese Rupee","₨","🇳🇵"), Currency("PHP","Philippine Peso","₱","🇵🇭"),
    Currency("VND","Vietnamese Dong","₫","🇻🇳"), Currency("PLN","Polish Zloty","zł","🇵🇱"),
)

private val FALLBACK: Map<String,Double> = mapOf(
    "USD" to 1.0,"EUR" to 0.92,"GBP" to 0.79,"INR" to 83.12,"JPY" to 149.50,
    "CNY" to 7.24,"AED" to 3.67,"SGD" to 1.34,"CAD" to 1.36,"AUD" to 1.53,
    "CHF" to 0.90,"SAR" to 3.75,"MYR" to 4.71,"BRL" to 4.97,"MXN" to 17.15,
    "KRW" to 1325.0,"ZAR" to 18.63,"TRY" to 32.40,"THB" to 35.12,"IDR" to 15750.0,
    "PKR" to 278.5,"BDT" to 110.0,"NZD" to 1.63,"HKD" to 7.83,"SEK" to 10.42,
    "NOK" to 10.55,"DKK" to 6.88,"RUB" to 89.50,"EGP" to 30.90,"NGN" to 1560.0,
    "QAR" to 3.64,"KWD" to 0.307,"OMR" to 0.385,"LKR" to 310.0,"NPR" to 133.0,
    "PHP" to 56.0,"VND" to 24500.0,"PLN" to 4.02,
)

data class CurrencyUiState(
    val fromIndex:Int=0, val toIndex:Int=3, val input:String="",
    val rates:Map<String,Double>=emptyMap(), val isLoading:Boolean=false,
    val ratesError:String?=null, val lastUpdated:String="", val copiedText:String?=null,
)

class CurrencyViewModel : ViewModel() {
    private val _s = MutableStateFlow(CurrencyUiState())
    val state = _s.asStateFlow()
    init { fetch() }
    fun onInput(v:String)  { _s.value=_s.value.copy(input=v) }
    fun onFrom(i:Int)      { _s.value=_s.value.copy(fromIndex=i) }
    fun onTo(i:Int)        { _s.value=_s.value.copy(toIndex=i) }
    fun swap()             { _s.value=_s.value.copy(fromIndex=_s.value.toIndex, toIndex=_s.value.fromIndex) }
    fun copied(t:String)   { _s.value=_s.value.copy(copiedText=t); viewModelScope.launch { kotlinx.coroutines.delay(2000); _s.value=_s.value.copy(copiedText=null) } }
    fun fetch() {
        viewModelScope.launch {
            _s.value=_s.value.copy(isLoading=true,ratesError=null)
            try {
                val codes=CURRENCIES.filter{it.code!="USD"}.joinToString(","){ it.code }
                val conn=(URL("https://api.frankfurter.app/latest?from=USD&to=$codes").openConnection() as HttpURLConnection).also{it.connectTimeout=8000;it.readTimeout=8000}
                val json=JSONObject(conn.inputStream.bufferedReader().readText())
                val r=mutableMapOf("USD" to 1.0)
                val ro=json.getJSONObject("rates"); ro.keys().forEach{ k->r[k]=ro.getDouble(k) }
                _s.value=_s.value.copy(rates=r,isLoading=false,lastUpdated="Live · ${json.optString("date","")}")
            } catch(e:Exception) { _s.value=_s.value.copy(rates=FALLBACK,isLoading=false,ratesError="Offline rates",lastUpdated="Offline") }
        }
    }
    fun convert(amount:Double,from:String,to:String):Double {
        val r=_s.value.rates; if(r.isEmpty()) return 0.0
        return (amount/(r[from]?:return 0.0))*(r[to]?:return 0.0)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyConverterScreen(onBack: () -> Unit) {
    val vm    = androidx.lifecycle.viewmodel.compose.viewModel<CurrencyViewModel>()
    val st    by vm.state.collectAsState()
    val clip  = LocalClipboardManager.current
    val from  = CURRENCIES[st.fromIndex]; val to = CURRENCIES[st.toIndex]
    val resultText = remember(st.input,st.fromIndex,st.toIndex,st.rates) {
        val c=vm.convert(st.input.toDoubleOrNull()?:0.0,from.code,to.code)
        if(c==0.0||st.input.isBlank()) "" else if(c>=1.0) "%.2f".format(c) else "%.6f".format(c).trimEnd('0').trimEnd('.')
    }
    Column(modifier=Modifier.fillMaxSize()) {
        NexusTopBar(title="Currency Converter",onBack=onBack,actions={ IconButton(onClick=vm::fetch){ Icon(Icons.Filled.Refresh,"Refresh") } })
        LazyColumn(Modifier.fillMaxSize(), contentPadding=PaddingValues(16.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item {
                Surface(color=if(st.ratesError!=null)MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,shape=MaterialTheme.shapes.small,modifier=Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal=12.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        if(st.isLoading) { CircularProgressIndicator(Modifier.size(12.dp),strokeWidth=1.5.dp); Text("Fetching live rates…",style=MaterialTheme.typography.labelSmall) }
                        else { Icon(if(st.ratesError!=null)Icons.Filled.WifiOff else Icons.Filled.CloudDone,null,Modifier.size(14.dp),tint=if(st.ratesError!=null)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onTertiaryContainer); Text(if(st.ratesError!=null)st.ratesError!! else "Rates: ${st.lastUpdated}",style=MaterialTheme.typography.labelSmall,color=if(st.ratesError!=null)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onTertiaryContainer) }
                    }
                }
            }
            item { OutlinedTextField(st.input,vm::onInput,Modifier.fillMaxWidth().semantics{contentDescription="Amount"},label={Text("Amount")},prefix={Text(from.symbol+" ")},placeholder={Text("0.00")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),singleLine=true) }
            item {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    CurrencyDrop("From",st.fromIndex,vm::onFrom,Modifier.weight(1f))
                    FilledTonalIconButton(onClick=vm::swap){ Icon(Icons.Filled.SwapVert,"Swap") }
                    CurrencyDrop("To",st.toIndex,vm::onTo,Modifier.weight(1f))
                }
            }
            item {
                AnimatedVisibility(resultText.isNotEmpty(),enter=fadeIn()+expandVertically(),exit=fadeOut()+shrinkVertically()) {
                    Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer),modifier=Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(20.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            Text(to.flag,style=MaterialTheme.typography.headlineSmall)
                            Column(Modifier.weight(1f)) {
                                Text("${to.symbol} $resultText",style=MaterialTheme.typography.headlineMedium.copy(fontWeight=FontWeight.ExtraBold),color=MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("${st.input.trim()} ${from.code} = $resultText ${to.code}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=0.7f))
                            }
                            FilledTonalIconButton(onClick={clip.setText(AnnotatedString(resultText));vm.copied(resultText)}) { Icon(if(st.copiedText==resultText)Icons.Filled.Check else Icons.Filled.ContentCopy,"Copy") }
                        }
                    }
                }
            }
            item { HorizontalDivider(); Spacer(Modifier.height(4.dp)); Text("All Rates — ${from.flag} ${from.code}",style=MaterialTheme.typography.labelLarge.copy(fontWeight=FontWeight.ExtraBold),color=MaterialTheme.colorScheme.primary,modifier=Modifier.padding(vertical=4.dp)) }
            val amt = st.input.toDoubleOrNull()?:1.0
            items(CURRENCIES) { cur ->
                val c=vm.convert(amt,from.code,cur.code)
                val d=if(c>=1.0)"%.2f".format(c) else "%.6f".format(c).trimEnd('0').trimEnd('.')
                Row(Modifier.fillMaxWidth().padding(vertical=9.dp).semantics{contentDescription="${cur.name}: ${cur.symbol} $d"},verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.weight(1f)) {
                        Text(cur.flag,style=MaterialTheme.typography.bodyMedium)
                        Column { Text(cur.code,style=MaterialTheme.typography.bodyMedium.copy(fontWeight=FontWeight.SemiBold)); Text(cur.name,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    Text("${cur.symbol} $d",style=MaterialTheme.typography.bodyMedium.copy(fontWeight=FontWeight.Bold),color=if(cur.code==to.code)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
                HorizontalDivider()
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyDrop(label:String,selected:Int,onSelect:(Int)->Unit,modifier:Modifier=Modifier) {
    var exp by remember{mutableStateOf(false)}
    val cur=CURRENCIES[selected]
    ExposedDropdownMenuBox(exp,{exp=it},modifier) {
        OutlinedTextField("${cur.flag} ${cur.code}",{},readOnly=true,label={Text(label)},trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(exp)},modifier=Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable,true).fillMaxWidth(),singleLine=true)
        ExposedDropdownMenu(exp,{exp=false},Modifier.heightIn(max=320.dp)) {
            CURRENCIES.forEachIndexed{i,c->DropdownMenuItem({Text("${c.flag} ${c.code} – ${c.name}")},{onSelect(i);exp=false})}
        }
    }
}
